package com.edumanage.tenant;

import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantContext {

    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long tenantId) {
        CONTEXT.set(tenantId);
    }

    public static Long getTenantId() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static boolean isSuperAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> {
                    var authority = a.getAuthority();
                    return "ROLE_SUPER_ADMIN".equalsIgnoreCase(authority) || "SUPER_ADMIN".equalsIgnoreCase(authority);
                });
    }
}
