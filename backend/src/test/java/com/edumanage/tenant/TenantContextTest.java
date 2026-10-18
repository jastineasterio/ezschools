package com.edumanage.tenant;

import org.junit.jupiter.api.Test;

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
}
