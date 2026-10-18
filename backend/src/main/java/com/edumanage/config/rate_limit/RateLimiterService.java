package com.edumanage.config.rate_limit;

public interface RateLimiterService {
    boolean tryConsume(String key);
    boolean tryConsume(String key, int tokens);
}
