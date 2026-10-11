package com.ecommerce.platform.security;

import com.ecommerce.platform.common.AuthRole;
import com.ecommerce.platform.common.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Creates and verifies short-lived stateless JWT access tokens.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpireMillis;
    private final long refreshExpireMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.access-expire-minutes:15}") long accessExpireMinutes,
                   @Value("${app.jwt.refresh-expire-days:7}") long refreshExpireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpireMillis = accessExpireMinutes * 60_000L;
        this.refreshExpireMillis = refreshExpireDays * 24 * 3600_000L;
    }

    public String generateToken(LoginUser user) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpireMillis))
                .signWith(key)
                .compact();
    }

    public LoginUser parseToken(String token) {
        Claims claims = parseClaims(token);
        LoginUser user = new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                AuthRole.valueOf(claims.get("role", String.class)));
        user.setJti(claims.getId());
        return user;
    }

    public long getAccessExpireSeconds() {
        return accessExpireMillis / 1000;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireMillis / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
