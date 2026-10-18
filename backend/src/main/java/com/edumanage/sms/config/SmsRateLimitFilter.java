package com.edumanage.sms.config;

import com.edumanage.sms.service.SmsMessageService;
import com.edumanage.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SmsRateLimitFilter extends OncePerRequestFilter {

    private static final int SMS_DAILY_LIMIT = 1000;
    private static final int SMS_HOURLY_LIMIT = 100;

    private static class SchoolRateLimit {
        int hourlyCount;
        int dailyCount;
        long hourlyReset;
        long dailyReset;
    }

    private final Map<Long, SchoolRateLimit> schoolLimits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().contains("/sms/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Long schoolId = TenantContext.getTenantId();
        if (schoolId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SchoolRateLimit limit = schoolLimits.computeIfAbsent(schoolId, k -> new SchoolRateLimit());
        long now = Instant.now().getEpochSecond();

        if (now - limit.hourlyReset >= 3600) {
            limit.hourlyCount = 0;
            limit.hourlyReset = now;
        }
        if (now - limit.dailyReset >= 86400) {
            limit.dailyCount = 0;
            limit.dailyReset = now;
        }

        if (limit.hourlyCount >= SMS_HOURLY_LIMIT) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"success\":false,\"message\":\"Hourly SMS limit exceeded\"}");
            return;
        }

        if (limit.dailyCount >= SMS_DAILY_LIMIT) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"success\":false,\"message\":\"Daily SMS limit exceeded\"}");
            return;
        }

        limit.hourlyCount++;
        limit.dailyCount++;

        filterChain.doFilter(request, response);
    }
}
