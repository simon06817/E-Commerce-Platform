package com.example.project01.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Cache helper implementing the read-through pattern with cache breakdown
 * protection and optional negative caching for missing records.
 */
@Component
@RequiredArgsConstructor
public class CacheSupport {

    private static final String NEGATIVE = "NEGATIVE";
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);
    private static final int WAIT_ROUNDS = 10;
    private static final long WAIT_MILLIS = 50;

    private final CacheManager cacheManager;
    private final CacheLockService lockService;

    public <T> T getOrLoad(String cacheName, String key, Supplier<T> loader) {
        // Fast path: serve directly from cache without taking the lock.
        Cache cache = cacheManager.getCache(cacheName);
        T cached = read(cache, key);
        if (cached != null) {
            return cached;
        }

        String lockKey = lockKey(cacheName, key);
        String token = lockService.newToken();
        // Only one request may rebuild a hot key; others wait and re-read cache.
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

    public <T> T getOrLoadWithNegativeCache(String positiveCacheName,
                                            String negativeCacheName,
                                            String key,
                                            Supplier<T> loader) {
        // Negative cache protects the database from repeated lookups of missing ids.
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

    public void put(String cacheName, String key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T read(Cache cache, String key) {
        if (cache == null) {
            return null;
        }
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper == null ? null : (T) wrapper.get();
    }

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

    private String lockKey(String cacheName, String key) {
        return "lock:" + cacheName + ":" + key;
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(WAIT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
