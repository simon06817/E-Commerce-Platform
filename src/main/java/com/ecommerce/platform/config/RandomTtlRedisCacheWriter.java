package com.ecommerce.platform.config;

import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/** 随机 TTL Redis 缓存写入器。 */
public class RandomTtlRedisCacheWriter implements RedisCacheWriter {

    private static final double MIN_FACTOR = 0.7;
    private static final double MAX_FACTOR = 1.3;

    private final RedisCacheWriter delegate;

    /** 包装缓存写入器。 */
    public RandomTtlRedisCacheWriter(RedisCacheWriter delegate) {
        this.delegate = delegate;
    }

    /** 写入缓存数据。 */
    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        delegate.put(name, key, value, randomize(ttl));
    }

    /** 读取缓存数据。 */
    @Override
    public byte[] get(String name, byte[] key) {
        return delegate.get(name, key);
    }

    /** 读取缓存数据。 */
    @Override
    public byte[] get(String name, byte[] key, Duration ttl) {
        return delegate.get(name, key, ttl);
    }

    /** 判断缓存是否支持异步读取。 */
    @Override
    public boolean supportsAsyncRetrieve() {
        return delegate.supportsAsyncRetrieve();
    }

    /** 异步读取缓存数据。 */
    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key) {
        return delegate.retrieve(name, key);
    }

    /** 异步读取缓存数据。 */
    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key, Duration ttl) {
        return delegate.retrieve(name, key, ttl);
    }

    /** 异步写入缓存数据。 */
    @Override
    public CompletableFuture<Void> store(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.store(name, key, value, randomize(ttl));
    }

    /** 缓存不存在时写入数据。 */
    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.putIfAbsent(name, key, value, randomize(ttl));
    }

    /** 删除缓存数据。 */
    @Override
    public void remove(String name, byte[] key) {
        delegate.remove(name, key);
    }

    /** 按模式清理缓存。 */
    @Override
    public void clean(String name, byte[] pattern) {
        delegate.clean(name, pattern);
    }

    /** 清理缓存统计信息。 */
    @Override
    public void clearStatistics(String name) {
        delegate.clearStatistics(name);
    }

    /** 包装缓存统计收集器。 */
    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector cacheStatisticsCollector) {
        return new RandomTtlRedisCacheWriter(delegate.withStatisticsCollector(cacheStatisticsCollector));
    }

    /** 获取缓存统计信息。 */
    @Override
    public CacheStatistics getCacheStatistics(String cacheName) {
        return delegate.getCacheStatistics(cacheName);
    }

    /** 为缓存 TTL 增加随机抖动。 */
    private Duration randomize(Duration ttl) {
        if (ttl == null || ttl.isZero()) {
            return ttl;
        }
        long millis = ttl.toMillis();
        double factor = MIN_FACTOR + ThreadLocalRandom.current().nextDouble() * (MAX_FACTOR - MIN_FACTOR);
        return Duration.ofMillis(Math.max(1, (long) (millis * factor)));
    }
}
