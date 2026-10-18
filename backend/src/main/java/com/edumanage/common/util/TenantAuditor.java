package com.edumanage.common.util;

import com.edumanage.security.CustomUserDetails;
import com.edumanage.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class TenantAuditor {

    public static Long currentTenantId() {
        return TenantContext.getTenantId();
    }

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.id();
        }
        return null;
    }
}
