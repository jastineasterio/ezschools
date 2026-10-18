package com.edumanage.sms.service;

import com.edumanage.sms.entity.SmsMessage;
import com.edumanage.sms.repository.SmsMessageRepository;
import com.edumanage.notifications.service.DarsmsProviderService;
import com.edumanage.notifications.service.NotificationProviderException;
import com.edumanage.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SmsWorker {

    private static final Logger log = LoggerFactory.getLogger(SmsWorker.class);

    private final SmsMessageRepository smsMessageRepository;
    private final SmsMessageService smsMessageService;
    private final DarsmsProviderService darsmsProviderService;

    public SmsWorker(SmsMessageRepository smsMessageRepository,
                     SmsMessageService smsMessageService,
                     DarsmsProviderService darsmsProviderService) {
        this.smsMessageRepository = smsMessageRepository;
        this.smsMessageService = smsMessageService;
        this.darsmsProviderService = darsmsProviderService;
    }

    @RabbitListener(queues = "${app.queues.sms}")
    public void processSmsJob(Long messageId) {
        SmsMessage message = smsMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("SMS message not found: " + messageId));

        Long schoolId = message.getSchoolId();
        TenantContext.setTenantId(schoolId);

        try {
            if (message.getRetryCount() >= 3) {
                smsMessageService.updateMessageStatus(messageId, "FAILED",
                        null, "SMS_MAX_RETRIES_EXCEEDED");
                return;
            }

            if (!darsmsProviderService.isConfigured()) {
                smsMessageService.updateMessageStatus(messageId, "FAILED",
                        null, "SMS_NOT_CONFIGURED");
                return;
            }

            String providerMessageId = darsmsProviderService.send(message.getRecipient(), message.getMessage());
            message.setProviderMessageId(providerMessageId);
            message.setProviderResponse("Accepted by DARSMS");
            message.setStatus("ACCEPTED");
            message.setSentAt(LocalDateTime.now());
            smsMessageRepository.save(message);
        } catch (NotificationProviderException e) {
            smsMessageService.updateMessageStatus(messageId, "FAILED",
                    null, e.getFailureCode());
        } finally {
            TenantContext.clear();
        }
    }
}
