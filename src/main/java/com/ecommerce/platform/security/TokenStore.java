package com.ecommerce.platform.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Refresh Token 存储。 */
@Component
public class TokenStore {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            """
            local value = redis.call('GET', KEYS[1])
            if not value then
                return nil
            end
            redis.call('DEL', KEYS[1])
            return value
            """,
            String.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final String cacheType;
    private final ConcurrentHashMap<String, LocalRefresh> localRefresh = new ConcurrentHashMap<>();

    /** 创建刷新令牌存储。 */
    public TokenStore(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                      @Value("${spring.cache.type:redis}") String cacheType) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.cacheType = cacheType;
    }

    /** 生成刷新令牌族标识。 */
    public String newFamilyId() {
        return UUID.randomUUID().toString();
    }

    /** 签发刷新令牌。 */
    public String issueRefresh(String familyId, String role, Long userId, String username,
                               String displayName, Duration ttl) {
        String rawToken = randomToken();
        String value = familyId + "|" + role + "|" + userId + "|" + username + "|" + displayName;
        String key = REFRESH_PREFIX + hash(rawToken);
        StringRedisTemplate template = redis();
        if (template != null) {
            template.opsForValue().set(key, value, ttl);
        } else {
            localRefresh.put(key, new LocalRefresh(value, System.currentTimeMillis() + ttl.toMillis()));
        }
        return rawToken;
    }

    /** 消费并轮换刷新令牌。 */
    public Optional<RefreshInfo> consumeRefresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        String key = REFRESH_PREFIX + hash(refreshToken);
        String value;
        StringRedisTemplate template = redis();
        if (template != null) {
            // 通过 Lua 脚本原子完成读取和删除，避免刷新令牌被并发重复使用。
            value = template.execute(CONSUME_SCRIPT, List.of(key));
        } else {
            // 非 Redis 环境仅用于本地测试。
            LocalRefresh local = localRefresh.remove(key);
            value = local == null || local.expiresAt() < System.currentTimeMillis()
                    ? null : local.value();
        }
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split("\\|", 5);
        if (parts.length != 5) {
            return Optional.empty();
        }
        try {
            return Optional.of(new RefreshInfo(
                    parts[0],
                    parts[1],
                    Long.valueOf(parts[2]),
                    parts[3],
                    parts[4]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** 删除刷新令牌。 */
    public void deleteRefresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String key = REFRESH_PREFIX + hash(refreshToken);
        StringRedisTemplate template = redis();
        if (template != null) {
            template.delete(key);
        } else {
            localRefresh.remove(key);
        }
    }

    /** 生成随机令牌。 */
    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 计算令牌摘要。 */
    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /** 获取 Redis 客户端。 */
    private StringRedisTemplate redis() {
        if (!"redis".equals(cacheType)) {
            return null;
        }
        return redisTemplateProvider.getIfAvailable();
    }

    public record RefreshInfo(String familyId, String role, Long userId,
                              String username, String displayName) {
    }

    private record LocalRefresh(String value, long expiresAt) {
    }
}
