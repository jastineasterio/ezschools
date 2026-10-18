package com.edumanage.sms.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.sms.dto.CreateSmsTemplateRequest;
import com.edumanage.sms.dto.SmsTemplateDto;
import com.edumanage.sms.entity.SmsTemplate;
import com.edumanage.sms.repository.SmsTemplateRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SmsTemplateService {

    private final SmsTemplateRepository smsTemplateRepository;
    private final AuditLogService auditLogService;

    public SmsTemplateService(SmsTemplateRepository smsTemplateRepository, AuditLogService auditLogService) {
        this.smsTemplateRepository = smsTemplateRepository;
        this.auditLogService = auditLogService;
    }

    public List<SmsTemplateDto> getTemplates() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return smsTemplateRepository.findBySchoolIdAndIsActive(tenantId, true).stream()
                .map(this::mapToDto)
                .toList();
    }

    public com.edumanage.common.dto.PageResponse<SmsTemplateDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        Page<SmsTemplate> page = smsTemplateRepository.findBySchoolIdAndIsActive(tenantId, true, pageable);
        return com.edumanage.common.dto.PageResponse.from(page.map(this::mapToDto));
    }

    public SmsTemplateDto getById(Long id) {
        var template = smsTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS template not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(template.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(template);
    }

    @Transactional
    public SmsTemplateDto create(CreateSmsTemplateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (smsTemplateRepository.existsBySchoolIdAndName(tenantId, request.getName())) {
            throw new ConflictException("SMS template with this name already exists");
        }

        SmsTemplate template = new SmsTemplate();
        template.setSchoolId(tenantId);
        template.setName(request.getName());
        template.setContent(request.getContent());
        template.setVariables(request.getVariables());
        template.setActive(request.isActive());

        var saved = smsTemplateRepository.save(template);

        auditLogService.log("SMS_TEMPLATE_CREATE", "SmsTemplate", saved.getId(),
                "Created SMS template " + request.getName(), null, null, null);

        return mapToDto(saved);
    }

    @Transactional
    public SmsTemplateDto update(Long id, CreateSmsTemplateRequest request) {
        var template = smsTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS template not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(template.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!template.getName().equals(request.getName()) &&
                smsTemplateRepository.existsBySchoolIdAndName(template.getSchoolId(), request.getName())) {
            throw new ConflictException("SMS template with this name already exists");
        }

        template.setName(request.getName());
        template.setContent(request.getContent());
        template.setVariables(request.getVariables());
        template.setActive(request.isActive());

        var saved = smsTemplateRepository.save(template);

        auditLogService.log("SMS_TEMPLATE_UPDATE", "SmsTemplate", saved.getId(),
                "Updated SMS template " + request.getName(), null, null, null);

        return mapToDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        var template = smsTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS template not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(template.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        smsTemplateRepository.delete(template);

        auditLogService.log("SMS_TEMPLATE_DELETE", "SmsTemplate", template.getId(),
                "Deleted SMS template " + template.getName(), null, null, null);
    }

    public SmsTemplate findRawById(Long id) {
        return smsTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
    }

    private SmsTemplateDto mapToDto(SmsTemplate template) {
        var dto = new SmsTemplateDto();
        dto.setId(template.getId());
        dto.setUuid(template.getUuid() != null ? template.getUuid().toString() : null);
        dto.setSchoolId(template.getSchoolId());
        dto.setName(template.getName());
        dto.setContent(template.getContent());
        dto.setVariables(template.getVariables());
        dto.setActive(template.isActive());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        return dto;
    }
}
