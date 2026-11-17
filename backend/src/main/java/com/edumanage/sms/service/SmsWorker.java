package com.edumanage.sms.service;

import com.edumanage.sms.entity.SmsMessage;
import com.edumanage.sms.repository.SmsMessageRepository;
import com.edumanage.notifications.service.DarsmsProviderService;
import com.edumanage.notifications.service.NotificationProviderException;
import com.edumanage.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SmsWorker {

    private static final Logger log = LoggerFactory.getLogger(SmsWorker.class);
    private static final int MAX_RETRIES = 3;

    private final SmsMessageRepository smsMessageRepository;
    private final SmsMessageService smsMessageService;
    private final SmsMessagePublisher smsMessagePublisher;
    private final DarsmsProviderService darsmsProviderService;
    private final SmsConfigurationService smsConfigurationService;
    private final ApplicationEventPublisher eventPublisher;

    public SmsWorker(SmsMessageRepository smsMessageRepository,
                     SmsMessageService smsMessageService,
                     SmsMessagePublisher smsMessagePublisher,
                     DarsmsProviderService darsmsProviderService,
                     SmsConfigurationService smsConfigurationService,
                     ApplicationEventPublisher eventPublisher) {
        this.smsMessageRepository = smsMessageRepository;
        this.smsMessageService = smsMessageService;
        this.smsMessagePublisher = smsMessagePublisher;
        this.darsmsProviderService = darsmsProviderService;
        this.smsConfigurationService = smsConfigurationService;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = "${app.queues.sms}")
    public void processSmsJob(Long messageId) {
        SmsMessage message = smsMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("SMS message not found: " + messageId));

        Long schoolId = message.getSchoolId();
        TenantContext.setTenantId(schoolId);

        try {
            String status = message.getStatus();
            if ("ACCEPTED".equals(status) || "SENT".equals(status) || "DELIVERED".equals(status) || "FAILED".equals(status)) {
                return;
            }

            if (message.getRetryCount() >= MAX_RETRIES) {
                smsMessageService.updateMessageStatus(messageId, "FAILED", null, "SMS_MAX_RETRIES_EXCEEDED");
                return;
            }

            SmsConfigurationService.SmsCredentials creds;
            try {
                creds = smsConfigurationService.getCredentials(schoolId);
            } catch (Exception e) {
                smsMessageService.updateMessageStatus(messageId, "FAILED", null, "SMS_NOT_CONFIGURED");
                return;
            }

            String normalized;
            try {
                normalized = DarsmsProviderService.normalizeTanzanianPhone(message.getRecipient());
            } catch (NotificationProviderException e) {
                smsMessageService.updateMessageStatus(messageId, "FAILED", null, e.getFailureCode());
                return;
            }

            DarsmsProviderService.SendResult sendResult = darsmsProviderService.send(normalized,
                    message.getMessage(), creds.apiKey(), creds.senderId());
            message.setProviderMessageId(sendResult.messageId());
            message.setBatchId(sendResult.batchId());
            message.setProviderResponse("Accepted by DARSMS");
            message.setStatus("ACCEPTED");
            message.setSentAt(LocalDateTime.now());
            smsMessageRepository.saveAndFlush(message);
            eventPublisher.publishEvent(new SmsMessageStatusEvent(message.getId(), schoolId, "ACCEPTED"));
        } catch (NotificationProviderException e) {
            String code = e.getFailureCode();
            boolean retryable = "SMS_PROVIDER_RATE_LIMITED".equals(code)
                    || "SMS_PROVIDER_UNAVAILABLE".equals(code);
            int next = message.getRetryCount() + 1;
            if (retryable && next < MAX_RETRIES) {
                message.setRetryCount(next);
                smsMessageRepository.save(message);
                smsMessagePublisher.publishSmsJob(messageId);
            } else {
                smsMessageService.updateMessageStatus(messageId, "FAILED", null, code);
            }
        } finally {
            TenantContext.clear();
        }
    }
}
