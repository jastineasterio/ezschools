package com.edumanage.tenant;

import com.edumanage.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextSecurityTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsSuperAdminReturnsFalseWhenUnauthenticated() {
        assertFalse(TenantContext.isSuperAdmin());
    }

    @Test
    void testIsSuperAdminReturnsFalseForManagerRole() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new CustomUserDetails(1L, "manager@test.com", "pass", 1L, true, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertFalse(TenantContext.isSuperAdmin());
    }

    @Test
    void testIsSuperAdminReturnsTrueForSuperAdminRole() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new CustomUserDetails(0L, "admin@test.com", "pass", null, true, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(TenantContext.isSuperAdmin());
    }
}
