package com.edumanage.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SmsRateLimitService {

    @Value("${app.sms.rate-limit.max-per-minute:60}")
    private int maxPerMinute;

    private final Map<Long, RateBucket> buckets = new ConcurrentHashMap<>();

    public boolean allowSend(Long schoolId) {
        var bucket = buckets.computeIfAbsent(schoolId, k -> new RateBucket());
        return bucket.tryConsume();
    }

    private static class RateBucket {
        private final AtomicInteger tokens;
        private long lastRefill;

        RateBucket() {
            this.tokens = new AtomicInteger(60);
            this.lastRefill = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            var now = System.currentTimeMillis();
            var elapsed = now - lastRefill;
            if (elapsed >= 60_000) {
                tokens.set(60);
                lastRefill = now;
            }
        }
    }
}
