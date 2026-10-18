package com.edumanage.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(SmsMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.queues.sms}")
    private String smsQueue;

    public SmsMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishSmsJob(Long messageId) {
        log.info("Publishing SMS job for messageId={}", messageId);
        rabbitTemplate.convertAndSend(smsQueue, messageId);
    }
}
