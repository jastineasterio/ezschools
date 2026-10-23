package com.edumanage.security;

import com.edumanage.exception.UnauthorizedException;
import com.edumanage.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtAuthFilterTest {

    @BeforeEach
    void clearStateBeforeTest() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void clearState() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void validBearerTokenSetsAuthenticationAndTenantContext() throws Exception {
        JwtService jwtService = Mockito.mock(JwtService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = Jwts.claims().add("schoolId", 7L).build();
        Mockito.when(jwtService.extractUsername("valid-token")).thenReturn("manager@example.test");
        Mockito.when(jwtService.isTokenExpired("valid-token")).thenReturn(false);
        Mockito.when(jwtService.extractClaims("valid-token")).thenReturn(claims);
        Mockito.when(jwtService.extractRoles("valid-token")).thenReturn(List.of("MANAGER"));

        FilterChain chain = (req, res) -> {
            assertEquals(7L, TenantContext.getTenantId());
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        };

        filter.doFilter(request, response, chain);

        assertNull(TenantContext.getTenantId());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidBearerTokenDoesNotCreateTenantContextOrAuthenticatedUser() throws Exception {
        JwtService jwtService = Mockito.mock(JwtService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Mockito.when(jwtService.extractUsername("invalid-token"))
                .thenThrow(new UnauthorizedException("Invalid or expired token"));

        FilterChain chain = (req, res) -> {
            assertNull(TenantContext.getTenantId());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        };

        filter.doFilter(request, response, chain);

        assertNull(TenantContext.getTenantId());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
