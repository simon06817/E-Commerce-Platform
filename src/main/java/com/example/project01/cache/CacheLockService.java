package com.example.project01.cache;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CacheLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ConcurrentHashMap<String, String> localLocks = new ConcurrentHashMap<>();
    private final String cacheType;

    public CacheLockService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                            @Value("${spring.cache.type:redis}") String cacheType) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.cacheType = cacheType;
    }

    public boolean tryLock(String key, String token, Duration ttl) {
        if ("redis".equals(cacheType)) {
            StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
            if (template != null) {
                Boolean acquired = template.opsForValue().setIfAbsent(key, token, ttl);
                return Boolean.TRUE.equals(acquired);
            }
        }
        return localLocks.putIfAbsent(key, token) == null;
    }

    public void unlock(String key, String token) {
        if ("redis".equals(cacheType)) {
            StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
            if (template != null) {
                template.execute(UNLOCK_SCRIPT, List.of(key), token);
                return;
            }
        }
        localLocks.remove(key, token);
    }

    public String newToken() {
        return UUID.randomUUID().toString();
    }
}
