package com.edumanage.config.queue;

import org.springframework.amqp.core.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMQRetryConfig {

    @Bean
    public RetryTemplate rabbitRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }
}
