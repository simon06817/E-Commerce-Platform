package com.ecommerce.platform.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/** 缓存支持组件。 */
@Component
@RequiredArgsConstructor
public class CacheSupport {

    private static final String NEGATIVE = "NEGATIVE";
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);
    private static final int WAIT_ROUNDS = 10;
    private static final long WAIT_MILLIS = 50;

    private final CacheManager cacheManager;
    private final CacheLockService lockService;

    /** 缓存未命中时加载数据。 */
    public <T> T getOrLoad(String cacheName, String key, Supplier<T> loader) {
        // 缓存命中时直接返回，不获取锁。
        Cache cache = cacheManager.getCache(cacheName);
        T cached = read(cache, key);
        if (cached != null) {
            return cached;
        }

        String lockKey = lockKey(cacheName, key);
        String token = lockService.newToken();
        // 热点键仅允许一个请求回源，其余请求等待后重读缓存。
        if (lockService.tryLock(lockKey, token, LOCK_TTL)) {
            try {
                T doubleCheck = read(cache, key);
                if (doubleCheck != null) {
                    return doubleCheck;
                }
                T value = loader.get();
                if (value != null && cache != null) {
                    cache.put(key, value);
                }
                return value;
            } finally {
                lockService.unlock(lockKey, token);
            }
        }

        T waited = waitForCache(cache, null, key);
        return waited != null ? waited : loader.get();
    }

    /** 通过正负缓存加载商品数据。 */
    public <T> T getOrLoadWithNegativeCache(String positiveCacheName,
                                            String negativeCacheName,
                                            String key,
                                            Supplier<T> loader) {
        // 负缓存避免不存在的 ID 反复查询数据库。
        Cache positive = cacheManager.getCache(positiveCacheName);
        Cache negative = cacheManager.getCache(negativeCacheName);

        T cached = read(positive, key);
        if (cached != null) {
            return cached;
        }
        if (negative != null && negative.get(key) != null) {
            return null;
        }

        String lockKey = lockKey(positiveCacheName, key);
        String token = lockService.newToken();
        if (lockService.tryLock(lockKey, token, LOCK_TTL)) {
            try {
                T doubleCheck = read(positive, key);
                if (doubleCheck != null) {
                    return doubleCheck;
                }
                if (negative != null && negative.get(key) != null) {
                    return null;
                }
                T value = loader.get();
                if (value != null) {
                    if (positive != null) {
                        positive.put(key, value);
                    }
                } else if (negative != null) {
                    negative.put(key, NEGATIVE);
                }
                return value;
            } finally {
                lockService.unlock(lockKey, token);
            }
        }

        T waited = waitForCache(positive, negative, key);
        if (waited != null) {
            return waited;
        }
        if (negative != null && negative.get(key) != null) {
            return null;
        }
        return loader.get();
    }

    /** 写入缓存数据。 */
    public void put(String cacheName, String key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /** 读取缓存数据。 */
    @SuppressWarnings("unchecked")
    private <T> T read(Cache cache, String key) {
        if (cache == null) {
            return null;
        }
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper == null ? null : (T) wrapper.get();
    }

    /** 等待其他请求回源后重读缓存。 */
    private <T> T waitForCache(Cache positive, Cache negative, String key) {
        for (int i = 0; i < WAIT_ROUNDS; i++) {
            sleepQuietly();
            T value = read(positive, key);
            if (value != null) {
                return value;
            }
            if (negative != null && negative.get(key) != null) {
                return null;
            }
        }
        return null;
    }

    /** 生成缓存锁键。 */
    private String lockKey(String cacheName, String key) {
        return "lock:" + cacheName + ":" + key;
    }

    /** 等待并忽略线程中断。 */
    private void sleepQuietly() {
        try {
            Thread.sleep(WAIT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
