package com.edumanage.config.rate_limit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = getRateLimitKey(request);
        if (!rateLimiterService.tryConsume(key)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded\",\"timestamp\":\"" + java.time.LocalDateTime.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}");
            return false;
        }
        return true;
    }

    private String getRateLimitKey(HttpServletRequest request) {
        String userId = extractUserId(request);
        String schoolId = extractSchoolId(request);
        String ip = getClientIp(request);
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (userId != null) {
            return "user:" + userId + ":" + method + ":" + path;
        }
        if (schoolId != null) {
            return "school:" + schoolId + ":" + method + ":" + path;
        }
        return "ip:" + ip + ":" + method + ":" + path;
    }

    private String extractUserId(HttpServletRequest request) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            return String.valueOf(((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id());
        }
        return null;
    }

    private String extractSchoolId(HttpServletRequest request) {
        Long tenantId = com.edumanage.tenant.TenantContext.getTenantId();
        return tenantId != null ? String.valueOf(tenantId) : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
