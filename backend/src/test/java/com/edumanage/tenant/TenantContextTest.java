package com.edumanage.tenant;

import com.edumanage.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @Test
    void testTenantContextSetAndGet() {
        TenantContext.setTenantId(1L);
        assertEquals(1L, TenantContext.getTenantId());
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testIsSuperAdminWhenNoAuth() {
        assertFalse(TenantContext.isSuperAdmin());
    }

    @Test
    void requiresSchoolAssignmentForTenantOperations() {
        TenantContext.clear();

        assertThrows(ForbiddenException.class, TenantContext::requireTenantId);
    }

    @Test
    void managerCannotAccessDifferentSchool() {
        TenantContext.setTenantId(1L);

        assertThrows(ForbiddenException.class, () -> TenantContext.assertTenantAccess(2L));
        assertDoesNotThrow(() -> TenantContext.assertTenantAccess(1L));
        TenantContext.clear();
    }

    @Test
    void superAdminRetainsCrossSchoolAccess() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@example.test", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            assertDoesNotThrow(() -> TenantContext.assertTenantAccess(2L));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
