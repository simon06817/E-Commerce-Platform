package com.example.project01.security;

import com.example.project01.common.AuthRole;
import com.example.project01.common.LoginUser;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private final JwtUtil jwtUtil =
            new JwtUtil("ECommercePlatformJwtSecretKey2026ForInternshipProject", 30, 7);

    @Test
    void generateAndParseToken() {
        LoginUser user = new LoginUser(1L, "buyer01", AuthRole.BUYER);
        String token = jwtUtil.generateToken(user);

        LoginUser parsed = jwtUtil.parseToken(token);

        assertEquals(user.getId(), parsed.getId());
        assertEquals(user.getUsername(), parsed.getUsername());
        assertEquals(user.getRole(), parsed.getRole());
        assertEquals(false, parsed.getJti() == null || parsed.getJti().isBlank());
    }

    @Test
    void invalidTokenThrows() {
        assertThrows(JwtException.class, () -> jwtUtil.parseToken("not-a-jwt"));
    }
}
