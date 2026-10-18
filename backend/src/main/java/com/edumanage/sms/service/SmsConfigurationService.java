package com.edumanage.sms.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.sms.dto.CreateSmsConfigurationRequest;
import com.edumanage.sms.dto.SmsConfigurationDto;
import com.edumanage.sms.entity.SmsConfiguration;
import com.edumanage.sms.repository.SmsConfigurationRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SmsConfigurationService {

    private final SmsConfigurationRepository smsConfigurationRepository;
    private final SmsEncryptionService smsEncryptionService;
    private final AuditLogService auditLogService;

    public SmsConfigurationService(SmsConfigurationRepository smsConfigurationRepository,
                                   SmsEncryptionService smsEncryptionService,
                                   AuditLogService auditLogService) {
        this.smsConfigurationRepository = smsConfigurationRepository;
        this.smsEncryptionService = smsEncryptionService;
        this.auditLogService = auditLogService;
    }

    public com.edumanage.common.dto.PageResponse<SmsConfigurationDto> getAllConfigurations(Pageable pageable) {
        Page<SmsConfiguration> page = smsConfigurationRepository.findAll(pageable);
        return com.edumanage.common.dto.PageResponse.from(page.map(this::mapToDto));
    }

    public List<SmsConfigurationDto> getAllConfigurationsList() {
        return smsConfigurationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public SmsConfigurationDto getConfiguration(Long id) {
        var config = smsConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS configuration not found"));
        return mapToDto(config);
    }

    public SmsConfigurationDto getConfigurationForSchool(Long schoolId) {
        var config = smsConfigurationRepository.findBySchoolIdAndIsActive(schoolId, true)
                .orElse(null);
        return config != null ? mapToDto(config) : null;
    }

    @Transactional
    public SmsConfigurationDto createConfiguration(CreateSmsConfigurationRequest request) {
        if (smsConfigurationRepository.existsBySchoolId(request.getSchoolId())) {
            throw new ConflictException("SMS configuration already exists for this school");
        }

        SmsConfiguration config = new SmsConfiguration();
        config.setSchoolId(request.getSchoolId());
        config.setProvider(request.getProvider());
        config.setEncryptedApiKey(smsEncryptionService.encrypt(request.getApiKey()));
        config.setEncryptedApiSecret(smsEncryptionService.encrypt(request.getApiSecret()));
        config.setSenderId(request.getSenderId());
        config.setMonthlyLimit(request.getMonthlyLimit());
        config.setDailyLimit(request.getDailyLimit());
        config.setRemarks(request.getRemarks());
        config.setActive(request.isActive());

        var saved = smsConfigurationRepository.save(config);

        auditLogService.log("SMS_CONFIG_CREATE", "SmsConfiguration", saved.getId(),
                "Created SMS configuration for school " + request.getSchoolId(), null, null, null);

        return mapToDto(saved);
    }

    @Transactional
    public SmsConfigurationDto updateConfiguration(Long id, CreateSmsConfigurationRequest request) {
        var config = smsConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS configuration not found"));

        config.setProvider(request.getProvider());
        config.setEncryptedApiKey(smsEncryptionService.encrypt(request.getApiKey()));
        config.setEncryptedApiSecret(smsEncryptionService.encrypt(request.getApiSecret()));
        if (request.getSenderId() != null) {
            config.setSenderId(request.getSenderId());
        }
        config.setMonthlyLimit(request.getMonthlyLimit());
        config.setDailyLimit(request.getDailyLimit());
        config.setRemarks(request.getRemarks());
        config.setActive(request.isActive());

        var saved = smsConfigurationRepository.save(config);

        auditLogService.log("SMS_CONFIG_UPDATE", "SmsConfiguration", saved.getId(),
                "Updated SMS configuration", null, null, null);

        return mapToDto(saved);
    }

    @Transactional
    public void deleteConfiguration(Long id) {
        var config = smsConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS configuration not found"));

        smsConfigurationRepository.delete(config);

        auditLogService.log("SMS_CONFIG_DELETE", "SmsConfiguration", config.getId(),
                "Deleted SMS configuration", null, null, null);
    }

    public SmsConfiguration findRawById(Long id) {
        return smsConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS configuration not found"));
    }

    public SmsConfiguration findRawBySchoolId(Long schoolId) {
        return smsConfigurationRepository.findBySchoolIdAndIsActive(schoolId, true)
                .orElseThrow(() -> new ResourceNotFoundException("No active SMS configuration found for school"));
    }

    private SmsConfigurationDto mapToDto(SmsConfiguration config) {
        var dto = new SmsConfigurationDto();
        dto.setId(config.getId());
        dto.setUuid(config.getUuid() != null ? config.getUuid().toString() : null);
        dto.setSchoolId(config.getSchoolId());
        dto.setProvider(config.getProvider());
        dto.setSenderId(config.getSenderId());
        dto.setMonthlyLimit(config.getMonthlyLimit());
        dto.setDailyLimit(config.getDailyLimit());
        dto.setRemarks(config.getRemarks());
        dto.setActive(config.isActive());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }
}
