package com.example.project01.config;

import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Delegating cache writer that adds random jitter to every TTL so that cached keys
 * do not all expire at the same moment (cache avalanche protection).
 */
public class RandomTtlRedisCacheWriter implements RedisCacheWriter {

    private static final double MIN_FACTOR = 0.7;
    private static final double MAX_FACTOR = 1.3;

    private final RedisCacheWriter delegate;

    public RandomTtlRedisCacheWriter(RedisCacheWriter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        delegate.put(name, key, value, randomize(ttl));
    }

    @Override
    public byte[] get(String name, byte[] key) {
        return delegate.get(name, key);
    }

    @Override
    public byte[] get(String name, byte[] key, Duration ttl) {
        return delegate.get(name, key, ttl);
    }

    @Override
    public boolean supportsAsyncRetrieve() {
        return delegate.supportsAsyncRetrieve();
    }

    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key) {
        return delegate.retrieve(name, key);
    }

    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key, Duration ttl) {
        return delegate.retrieve(name, key, ttl);
    }

    @Override
    public CompletableFuture<Void> store(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.store(name, key, value, randomize(ttl));
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.putIfAbsent(name, key, value, randomize(ttl));
    }

    @Override
    public void remove(String name, byte[] key) {
        delegate.remove(name, key);
    }

    @Override
    public void clean(String name, byte[] pattern) {
        delegate.clean(name, pattern);
    }

    @Override
    public void clearStatistics(String name) {
        delegate.clearStatistics(name);
    }

    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector cacheStatisticsCollector) {
        return new RandomTtlRedisCacheWriter(delegate.withStatisticsCollector(cacheStatisticsCollector));
    }

    @Override
    public CacheStatistics getCacheStatistics(String cacheName) {
        return delegate.getCacheStatistics(cacheName);
    }

    private Duration randomize(Duration ttl) {
        if (ttl == null || ttl.isZero()) {
            return ttl;
        }
        long millis = ttl.toMillis();
        double factor = MIN_FACTOR + ThreadLocalRandom.current().nextDouble() * (MAX_FACTOR - MIN_FACTOR);
        return Duration.ofMillis(Math.max(1, (long) (millis * factor)));
    }
}
