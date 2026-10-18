package com.edumanage.config;

import com.edumanage.config.rate_limit.RateLimitProperties;
import com.edumanage.config.rate_limit.RateLimiterService;
import com.edumanage.config.rate_limit.SlidingWindowRateLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public RateLimiterService rateLimiterService(RateLimitProperties properties) {
        return new SlidingWindowRateLimiter(properties);
    }
}
