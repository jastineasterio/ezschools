package com.edumanage.tenant;

import com.edumanage.exception.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public final class TenantContext {

    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long tenantId) {
        CONTEXT.set(tenantId);
    }

    public static Long getTenantId() {
        return CONTEXT.get();
    }

    public static Long requireTenantId() {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("A school assignment is required");
        }
        return tenantId;
    }

    public static void assertTenantAccess(Long schoolId) {
        if (!isSuperAdmin() && !Objects.equals(getTenantId(), schoolId)) {
            throw new ForbiddenException("Access denied");
        }
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
