package com.edumanage.sms.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.util.AuditAware;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.notifications.service.DarsmsProviderService;
import com.edumanage.notifications.service.NotificationProviderException;
import com.edumanage.sms.dto.CreateSmsMessageRequest;
import com.edumanage.sms.dto.SmsMessageDto;
import com.edumanage.sms.entity.SmsMessage;
import com.edumanage.sms.entity.SmsTemplate;
import com.edumanage.sms.repository.SmsMessageRepository;
import com.edumanage.sms.repository.SmsTemplateRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class SmsMessageService {

    private final SmsMessageRepository smsMessageRepository;
    private final SmsTemplateRepository smsTemplateRepository;
    private final SmsMessagePublisher smsMessagePublisher;
    private final AuditLogService auditLogService;
    private final SmsRateLimitService smsRateLimitService;
    private final SmsConfigurationService smsConfigurationService;
    private final DarsmsProviderService darsmsProviderService;
    private final ApplicationEventPublisher eventPublisher;

    public SmsMessageService(SmsMessageRepository smsMessageRepository,
                             SmsTemplateRepository smsTemplateRepository,
                             SmsMessagePublisher smsMessagePublisher,
                             AuditLogService auditLogService,
                             SmsRateLimitService smsRateLimitService,
                             SmsConfigurationService smsConfigurationService,
                             DarsmsProviderService darsmsProviderService,
                             ApplicationEventPublisher eventPublisher) {
        this.smsMessageRepository = smsMessageRepository;
        this.smsTemplateRepository = smsTemplateRepository;
        this.smsMessagePublisher = smsMessagePublisher;
        this.auditLogService = auditLogService;
        this.smsRateLimitService = smsRateLimitService;
        this.smsConfigurationService = smsConfigurationService;
        this.darsmsProviderService = darsmsProviderService;
        this.eventPublisher = eventPublisher;
    }

    public Page<SmsMessageDto> getMessages(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("School context required");
        }
        return smsMessageRepository.findBySchoolId(tenantId, pageable)
                .map(this::mapToDto);
    }

    public Page<SmsMessageDto> getMessagesByStatus(String status, Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("School context required");
        }
        return smsMessageRepository.findBySchoolIdAndStatus(tenantId, status, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public SmsMessageDto sendSms(CreateSmsMessageRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("School context required");
        }

        if (!smsRateLimitService.allowSend(tenantId)) {
            throw new BadRequestException("Rate limit exceeded. Please try again later.");
        }

        String normalizedRecipient = validateRecipient(request.getRecipient());
        enforceSendingLimits(tenantId);

        String messageContent = request.getMessage();
        if (request.getTemplateId() != null) {
            SmsTemplate template = smsTemplateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
            if (!template.getSchoolId().equals(tenantId)) {
                throw new ForbiddenException("Access denied");
            }
            messageContent = template.getContent();
        }

        SmsMessage message = new SmsMessage();
        message.setSchoolId(tenantId);
        message.setRecipient(normalizedRecipient);
        message.setMessage(messageContent);
        message.setTemplateId(request.getTemplateId());
        message.setStatus("PENDING");
        message.setRetryCount(0);
        message.setCreatedBy(AuditAware.currentUserId());

        var saved = smsMessageRepository.save(message);

        auditLogService.log("SMS_SEND", "SmsMessage", saved.getId(),
                "SMS queued to " + request.getRecipient(), null, null, null);

        smsMessagePublisher.publishSmsJob(saved.getId());

        return mapToDto(saved);
    }

    @Transactional
    public List<SmsMessageDto> sendBulkSms(CreateSmsMessageRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("School context required");
        }

        if (!smsRateLimitService.allowSend(tenantId)) {
            throw new BadRequestException("Rate limit exceeded. Please try again later.");
        }

        enforceSendingLimits(tenantId);

        String[] recipients = request.getRecipient().split(",");
        List<SmsMessageDto> results = new ArrayList<>();

        for (String recipient : recipients) {
            String trimmed = recipient.trim();
            if (trimmed.isEmpty()) continue;

            String normalizedRecipient = validateRecipient(trimmed);

            if (!smsRateLimitService.allowSend(tenantId)) {
                break;
            }

            String messageContent = request.getMessage();
            if (request.getTemplateId() != null) {
                SmsTemplate template = smsTemplateRepository.findById(request.getTemplateId())
                        .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
                if (!template.getSchoolId().equals(tenantId)) {
                    throw new ForbiddenException("Access denied");
                }
                messageContent = template.getContent();
            }

            SmsMessage message = new SmsMessage();
            message.setSchoolId(tenantId);
            message.setRecipient(normalizedRecipient);
            message.setMessage(messageContent);
            message.setTemplateId(request.getTemplateId());
            message.setStatus("PENDING");
            message.setRetryCount(0);
            message.setCreatedBy(AuditAware.currentUserId());

            var saved = smsMessageRepository.save(message);
            smsMessagePublisher.publishSmsJob(saved.getId());
            results.add(mapToDto(saved));
        }

        auditLogService.log("SMS_BULK_SEND", "SmsMessage", null,
                "Bulk SMS queued to " + results.size() + " recipients", null, null, null);

        return results;
    }

    @Transactional
    public SmsMessageDto updateMessageStatus(Long id, String status, String providerResponse, String errorMessage) {
        var message = smsMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS message not found"));

        message.setStatus(status);
        message.setProviderResponse(providerResponse);

        if (errorMessage != null) {
            message.setErrorMessage(errorMessage);
        }

        if ("SENT".equals(status)) {
            message.setSentAt(LocalDateTime.now());
        } else if ("DELIVERED".equals(status)) {
            message.setDeliveredAt(LocalDateTime.now());
        } else if ("FAILED".equals(status)) {
            message.setFailedAt(LocalDateTime.now());
            message.setRetryCount(message.getRetryCount() + 1);
        }

        var saved = smsMessageRepository.save(message);
        publishStatusAfterCommit(saved);
        return mapToDto(saved);
    }

    private void publishStatusAfterCommit(SmsMessage message) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishEvent(
                                    new SmsMessageStatusEvent(message.getId(), message.getSchoolId(), message.getStatus()));
                        }
                    });
        } else {
            eventPublisher.publishEvent(
                    new SmsMessageStatusEvent(message.getId(), message.getSchoolId(), message.getStatus()));
        }
    }

    private String validateRecipient(String recipient) {
        try {
            return darsmsProviderService.normalizeTanzanianPhone(recipient);
        } catch (NotificationProviderException e) {
            throw new BadRequestException("Invalid recipient phone number: " + e.getFailureCode());
        }
    }

    private void enforceSendingLimits(Long schoolId) {
        com.edumanage.sms.entity.SmsConfiguration config;
        try {
            config = smsConfigurationService.findRawBySchoolId(schoolId);
        } catch (ResourceNotFoundException e) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (config.getDailyLimit() != null) {
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            long count = smsMessageRepository.countBySchoolIdAndCreatedAtBetween(schoolId, startOfDay, now);
            if (count >= config.getDailyLimit()) {
                throw new BadRequestException("Daily SMS limit reached for this school");
            }
        }
        if (config.getMonthlyLimit() != null) {
            YearMonth month = YearMonth.now();
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            long count = smsMessageRepository.countBySchoolIdAndCreatedAtBetween(schoolId, startOfMonth, now);
            if (count >= config.getMonthlyLimit()) {
                throw new BadRequestException("Monthly SMS limit reached for this school");
            }
        }
    }

    @Transactional
    public SmsMessageDto checkMessageStatus(Long messageId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("School context required");
        }
        SmsMessage message = smsMessageRepository.findBySchoolIdAndId(tenantId, messageId)
                .orElseThrow(() -> new ResourceNotFoundException("SMS message not found"));

        if (message.getProviderMessageId() == null) {
            return mapToDto(message);
        }

        SmsConfigurationService.SmsCredentials creds;
        try {
            creds = smsConfigurationService.getCredentials(tenantId);
        } catch (Exception e) {
            throw new BadRequestException("SMS not configured for this school");
        }

        DarsmsProviderService.MessageStatus providerStatus;
        try {
            providerStatus = darsmsProviderService.getMessageStatus(creds.apiKey(), message.getProviderMessageId());
        } catch (NotificationProviderException e) {
            throw new BadRequestException("Unable to fetch message status: " + e.getFailureCode());
        }

        String mapped = mapProviderStatus(providerStatus.status());
        if (mapped != null && !mapped.equals(message.getStatus())) {
            message.setStatus(mapped);
        }
        if (providerStatus.errorMessage() != null) {
            message.setErrorMessage(providerStatus.errorMessage());
        }
        if ("DELIVERED".equals(message.getStatus()) && message.getDeliveredAt() == null) {
            message.setDeliveredAt(LocalDateTime.now());
        } else if ("FAILED".equals(message.getStatus()) && message.getFailedAt() == null) {
            message.setFailedAt(LocalDateTime.now());
            message.setRetryCount(message.getRetryCount() + 1);
        }

        var saved = smsMessageRepository.save(message);
        return mapToDto(saved);
    }

    private String mapProviderStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "QUEUED" -> "PENDING";
            case "SENT" -> "SENT";
            case "DELIVERED" -> "DELIVERED";
            case "FAILED" -> "FAILED";
            default -> null;
        };
    }

    private SmsMessageDto mapToDto(SmsMessage message) {
        var dto = new SmsMessageDto();
        dto.setId(message.getId());
        dto.setUuid(message.getUuid() != null ? message.getUuid().toString() : null);
        dto.setSchoolId(message.getSchoolId());
        dto.setTemplateId(message.getTemplateId());
        dto.setRecipient(message.getRecipient());
        dto.setMessage(message.getMessage());
        dto.setStatus(message.getStatus());
        dto.setProviderResponse(message.getProviderResponse());
        dto.setProviderMessageId(message.getProviderMessageId());
        dto.setBatchId(message.getBatchId());
        dto.setRetryCount(message.getRetryCount());
        dto.setSentAt(message.getSentAt());
        dto.setDeliveredAt(message.getDeliveredAt());
        dto.setFailedAt(message.getFailedAt());
        dto.setErrorMessage(message.getErrorMessage());
        dto.setCreatedBy(message.getCreatedBy());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setUpdatedAt(message.getUpdatedAt());
        return dto;
    }
}
