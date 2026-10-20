package com.qingshan.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "qingshan-test-secret-key-0123456789abcdef";

    @Test
    void createAndParseTokenKeepsClaimsAndJti() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("empId", 7L);
        claims.put("roleCode", "admin");

        String token = JwtUtil.createJWT(SECRET, 60_000, claims);
        Claims parsed = JwtUtil.parseJWT(SECRET, token);

        assertEquals(7, ((Number) parsed.get("empId")).intValue());
        assertEquals("admin", parsed.get("roleCode", String.class));
        assertNotNull(parsed.getId(), "token 必须包含 jti，退出登录黑名单依赖该字段");
        assertNotNull(parsed.getExpiration());
    }

    @Test
    void parseRejectsTokenSignedWithAnotherKey() {
        String token = JwtUtil.createJWT(SECRET, 60_000, new HashMap<>());
        String otherSecret = "qingshan-other-secret-key-0123456789abcdef";

        assertThrows(JwtException.class, () -> JwtUtil.parseJWT(otherSecret, token));
    }

    @Test
    void parseRejectsExpiredToken() {
        String token = JwtUtil.createJWT(SECRET, -1_000, new HashMap<>());

        assertThrows(ExpiredJwtException.class, () -> JwtUtil.parseJWT(SECRET, token));
    }

    @Test
    void weakSecretIsRejected() {
        assertThrows(WeakKeyException.class,
                () -> JwtUtil.createJWT("short", 60_000, new HashMap<>()));
    }
}
