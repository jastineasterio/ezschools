package com.edumanage.sms.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.util.AuditAware;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.sms.dto.CreateSmsMessageRequest;
import com.edumanage.sms.dto.SmsMessageDto;
import com.edumanage.sms.entity.SmsMessage;
import com.edumanage.sms.entity.SmsTemplate;
import com.edumanage.sms.repository.SmsMessageRepository;
import com.edumanage.sms.repository.SmsTemplateRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SmsMessageService {

    private final SmsMessageRepository smsMessageRepository;
    private final SmsTemplateRepository smsTemplateRepository;
    private final SmsMessagePublisher smsMessagePublisher;
    private final AuditLogService auditLogService;
    private final SmsRateLimitService smsRateLimitService;

    public SmsMessageService(SmsMessageRepository smsMessageRepository,
                             SmsTemplateRepository smsTemplateRepository,
                             SmsMessagePublisher smsMessagePublisher,
                             AuditLogService auditLogService,
                             SmsRateLimitService smsRateLimitService) {
        this.smsMessageRepository = smsMessageRepository;
        this.smsTemplateRepository = smsTemplateRepository;
        this.smsMessagePublisher = smsMessagePublisher;
        this.auditLogService = auditLogService;
        this.smsRateLimitService = smsRateLimitService;
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

        String messageContent = request.getMessage();
        if (request.getTemplateId() != null) {
            SmsTemplate template = smsTemplateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
            if (!template.getSchoolId().equals(tenantId)) {
                throw new com.edumanage.exception.ForbiddenException("Access denied");
            }
            messageContent = template.getContent();
        }

        SmsMessage message = new SmsMessage();
        message.setSchoolId(tenantId);
        message.setRecipient(request.getRecipient());
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

        String[] recipients = request.getRecipient().split(",");
        List<SmsMessageDto> results = new ArrayList<>();

        for (String recipient : recipients) {
            String trimmed = recipient.trim();
            if (trimmed.isEmpty()) continue;

            if (!smsRateLimitService.allowSend(tenantId)) {
                break;
            }

            String messageContent = request.getMessage();
            if (request.getTemplateId() != null) {
                SmsTemplate template = smsTemplateRepository.findById(request.getTemplateId())
                        .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
                if (!template.getSchoolId().equals(tenantId)) {
                    throw new com.edumanage.exception.ForbiddenException("Access denied");
                }
                messageContent = template.getContent();
            }

            SmsMessage message = new SmsMessage();
            message.setSchoolId(tenantId);
            message.setRecipient(trimmed);
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
        return mapToDto(saved);
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
