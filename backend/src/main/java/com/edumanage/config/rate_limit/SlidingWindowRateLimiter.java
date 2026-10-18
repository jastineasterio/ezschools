package com.edumanage.config.rate_limit;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SlidingWindowRateLimiter implements RateLimiterService {

    private static final int DEFAULT_CAPACITY = 100;
    private static final int DEFAULT_REFILL_TOKENS = 100;
    private static final int DEFAULT_REFILL_PERIOD_SECONDS = 60;

    private final int capacity;
    private final int refillTokens;
    private final long refillPeriodNanos;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(RateLimitProperties properties) {
        this.capacity = properties.capacity() > 0 ? properties.capacity() : DEFAULT_CAPACITY;
        this.refillTokens = properties.refillTokens() > 0 ? properties.refillTokens() : DEFAULT_REFILL_TOKENS;
        this.refillPeriodNanos = (properties.refillPeriodSeconds() > 0 ? properties.refillPeriodSeconds() : DEFAULT_REFILL_PERIOD_SECONDS) * 1_000_000_000L;
    }

    @Override
    public boolean tryConsume(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillTokens, refillPeriodNanos));
        return bucket.tryConsume();
    }

    @Override
    public boolean tryConsume(String key, int tokens) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillTokens, refillPeriodNanos));
        return bucket.tryConsume(tokens);
    }

    private static class TokenBucket {
        private final int capacity;
        private final int refillTokens;
        private final long refillPeriodNanos;
        private double tokens;
        private long lastRefillTimestamp;

        TokenBucket(int capacity, int refillTokens, long refillPeriodNanos) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodNanos = refillPeriodNanos;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        synchronized boolean tryConsume(int count) {
            refill();
            if (tokens >= count) {
                tokens -= count;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillTimestamp;
            if (elapsed > 0) {
                long periods = elapsed / refillPeriodNanos;
                if (periods > 0) {
                    tokens = Math.min(capacity, tokens + periods * refillTokens);
                    lastRefillTimestamp += periods * refillPeriodNanos;
                }
            }
        }
    }
}
