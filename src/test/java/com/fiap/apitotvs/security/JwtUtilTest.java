package com.fiap.apitotvs.security;

import com.fiap.apitotvs.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "fiap-totvs-api-jwt-secret-key-change-me-in-production-2026");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86_400_000L);

        user = new User();
        user.setId(10L);
        user.setEmail("teste@totvs.com");
        user.setName("Usuario Teste");
        user.setPassword("encoded");
        user.setIsActive(true);
    }

    @Test
    void generateAndValidateToken() {
        String token = jwtUtil.generateToken(user, Map.of("userId", user.getId()));

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token, user));
        assertEquals("teste@totvs.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.isValidToken(token));
    }

    @Test
    void safeExtractUsernameRetornaNullParaTokenInvalido() {
        assertEquals(null, jwtUtil.safeExtractUsername("token.invalido"));
        assertFalse(jwtUtil.isValidToken(""));
        assertFalse(jwtUtil.isValidToken(null));
    }
}
