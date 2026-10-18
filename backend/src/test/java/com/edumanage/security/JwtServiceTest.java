package com.edumanage.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void extractRolesRoundTripsWithoutDoublePrefix() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "change-me-in-production-use-a-long-random-string-at-least-32-chars");
        ReflectionTestUtils.setField(jwtService, "accessTokenValidity", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenValidity", 604800000L);

        var authorities = List.of(new SimpleGrantedAuthority("SUPER_ADMIN"));
        String token = jwtService.generateAccessToken(
                new CustomUserDetails(2L, "admin@edumanage.com", "hash", null, true, authorities),
                2L, null, List.of("SUPER_ADMIN"));

        List<String> roles = jwtService.extractRoles(token);
        assertEquals(List.of("SUPER_ADMIN"), roles);
        assertFalse(jwtService.isTokenExpired(token));
        assertEquals("admin@edumanage.com", jwtService.extractUsername(token));
    }
}
