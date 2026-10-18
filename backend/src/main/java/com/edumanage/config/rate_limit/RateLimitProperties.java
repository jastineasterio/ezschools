package com.edumanage.config.rate_limit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("100") int capacity,
        @DefaultValue("60") int refillPeriodSeconds,
        @DefaultValue("100") int refillTokens
) {
}
