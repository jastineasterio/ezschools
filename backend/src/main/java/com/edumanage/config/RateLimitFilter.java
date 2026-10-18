package com.edumanage.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.capacity:100}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens:100}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-period-seconds:60}")
    private long refillPeriodSeconds;

    private static class TokenBucket {
        private final int refillTokens;
        private final long refillPeriodSeconds;
        double tokens;
        long lastRefill;

        TokenBucket(int refillTokens, long refillPeriodSeconds) {
            this.refillTokens = refillTokens;
            this.refillPeriodSeconds = refillPeriodSeconds;
            this.tokens = refillTokens;
            this.lastRefill = Instant.now().getEpochSecond();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            var now = Instant.now().getEpochSecond();
            var elapsed = now - lastRefill;
            if (elapsed >= refillPeriodSeconds) {
                tokens = refillTokens;
                lastRefill = now;
            }
        }
    }

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var key = getClientId(request);
        var bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(refillTokens, (int) refillPeriodSeconds));

        if (!bucket.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests\",\"timestamp\":\"" + Instant.now() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientId(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
