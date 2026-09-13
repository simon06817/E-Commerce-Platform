package com.example.project01.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores refresh tokens and the access-token blacklist.
 *
 * <p>Uses Redis when Redis caching is enabled and falls back to an in-process
 * map for tests. The in-process fallback intentionally loses data on restart.</p>
 */
@Component
public class TokenStore {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final String cacheType;
    private final ConcurrentHashMap<String, String> localRefresh = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> localBlacklist = new ConcurrentHashMap<>();

    public TokenStore(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                      @Value("${spring.cache.type:redis}") String cacheType) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.cacheType = cacheType;
    }

    public void saveRefresh(String refreshId, String role, Long userId, String username,
                            String displayName, Duration ttl) {
        String value = role + "|" + userId + "|" + username + "|" + displayName;
        StringRedisTemplate template = redis();
        if (template != null) {
            template.opsForValue().set(REFRESH_PREFIX + refreshId, value, ttl);
        } else {
            localRefresh.put(refreshId, value);
        }
    }

    public Optional<RefreshInfo> getRefresh(String refreshId) {
        String value;
        StringRedisTemplate template = redis();
        if (template != null) {
            value = template.opsForValue().get(REFRESH_PREFIX + refreshId);
        } else {
            value = localRefresh.get(refreshId);
        }
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split("\\|", 4);
        if (parts.length != 4) {
            return Optional.empty();
        }
        return Optional.of(new RefreshInfo(parts[0], Long.valueOf(parts[1]), parts[2], parts[3]));
    }

    public void deleteRefresh(String refreshId) {
        StringRedisTemplate template = redis();
        if (template != null) {
            template.delete(REFRESH_PREFIX + refreshId);
        } else {
            localRefresh.remove(refreshId);
        }
    }

    public void blacklistAccess(String jti, Duration ttl) {
        if (jti == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        StringRedisTemplate template = redis();
        if (template != null) {
            template.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl);
        } else {
            localBlacklist.put(jti, System.currentTimeMillis() + ttl.toMillis());
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        StringRedisTemplate template = redis();
        if (template != null) {
            return Boolean.TRUE.equals(template.hasKey(BLACKLIST_PREFIX + jti));
        }
        Long expiresAt = localBlacklist.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            localBlacklist.remove(jti);
            return false;
        }
        return true;
    }

    private StringRedisTemplate redis() {
        if (!"redis".equals(cacheType)) {
            return null;
        }
        return redisTemplateProvider.getIfAvailable();
    }

    public record RefreshInfo(String role, Long userId, String username, String displayName) {
    }
}
