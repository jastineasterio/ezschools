package com.edumanage.sms.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.notifications.service.DarsmsProviderService;
import com.edumanage.notifications.service.NotificationProviderException;
import com.edumanage.sms.dto.CreateSmsConfigurationRequest;
import com.edumanage.sms.dto.SmsConfigurationDto;
import com.edumanage.sms.dto.UpdateSmsConfigurationRequest;
import com.edumanage.sms.entity.SmsConfiguration;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.sms.repository.SmsConfigurationRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SmsConfigurationService {

    private final SmsConfigurationRepository smsConfigurationRepository;
    private final SmsEncryptionService smsEncryptionService;
    private final AuditLogService auditLogService;
    private final SchoolRepository schoolRepository;
    private final DarsmsProviderService darsmsProvider;

    public SmsConfigurationService(SmsConfigurationRepository smsConfigurationRepository,
                                   SmsEncryptionService smsEncryptionService,
                                   AuditLogService auditLogService,
                                   SchoolRepository schoolRepository,
                                   DarsmsProviderService darsmsProvider) {
        this.smsConfigurationRepository = smsConfigurationRepository;
        this.smsEncryptionService = smsEncryptionService;
        this.auditLogService = auditLogService;
        this.schoolRepository = schoolRepository;
        this.darsmsProvider = darsmsProvider;
    }

    public com.edumanage.common.dto.PageResponse<SmsConfigurationDto> getAllConfigurations(Pageable pageable) {
        Page<SmsConfiguration> page = smsConfigurationRepository.findByDeletedAtIsNull(pageable);
        return com.edumanage.common.dto.PageResponse.from(page.map(this::mapToDto));
    }

    public List<SmsConfigurationDto> getAllConfigurationsList() {
        return smsConfigurationRepository.findByDeletedAtIsNull(org.springframework.data.domain.PageRequest.of(0, 1000))
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public SmsConfigurationDto getConfiguration(Long id) {
        var config = smsConfigurationRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("SMS configuration not found"));
        return mapToDto(config);
    }

    public com.edumanage.common.dto.PageResponse<SmsConfigurationDto> getConfigurationsForSchool(Long schoolId, Pageable pageable) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found");
        }
        Page<SmsConfiguration> page = smsConfigurationRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, pageable);
        return com.edumanage.common.dto.PageResponse.from(page.map(this::mapToDto));
    }

    /**
     * Checks whether a school has an active, non-deleted DARSMS SMS configuration
     * with an encrypted API key and an approved Sender ID present. Does not decrypt
     * or expose any secret.
     */
    public boolean isConfigured(Long schoolId) {
        return smsConfigurationRepository.findBySchoolIdAndIsActiveAndDeletedAtIsNull(schoolId, true)
                .map(config -> config.getEncryptedApiKey() != null && !config.getEncryptedApiKey().isBlank()
                        && config.getSenderId() != null && !config.getSenderId().isBlank())
                .orElse(false);
    }

    /**
     * Returns true if any school has an active, non-deleted DARSMS SMS configuration
     * with an encrypted API key and a sender ID present. This is the same required
     * configuration the real DarsmsProviderService needs to send SMS. Never decrypts
     * or exposes any secret.
     */
    public boolean hasAnyConfiguredSchool() {
        return smsConfigurationRepository.findByDeletedAtIsNull(PageRequest.of(0, 1000)).stream()
                .filter(SmsConfiguration::isActive)
                .anyMatch(config -> config.getEncryptedApiKey() != null && !config.getEncryptedApiKey().isBlank()
                        && config.getSenderId() != null && !config.getSenderId().isBlank());
    }

    /**
     * Returns decrypted SMS credentials for a school. Decryption happens only here,
     * immediately before sending, and the returned value must never be written to a
     * DTO, log, audit record, or API response.
     */
    public SmsCredentials getCredentials(Long schoolId) {
        var config = smsConfigurationRepository.findBySchoolIdAndIsActiveAndDeletedAtIsNull(schoolId, true)
                .orElseThrow(() -> new ResourceNotFoundException("No active SMS configuration found for school"));
        String apiKey = smsEncryptionService.decrypt(config.getEncryptedApiKey());
        String apiSecret = config.getEncryptedApiSecret() != null && !config.getEncryptedApiSecret().isBlank()
                ? smsEncryptionService.decrypt(config.getEncryptedApiSecret())
                : null;
        return new SmsCredentials(apiKey, apiSecret, config.getSenderId());
    }

    public List<String> getApprovedSenderIds(Long schoolId) {
        SmsCredentials creds = getCredentials(schoolId);
        return darsmsProvider.getApprovedSenderIds(creds.apiKey());
    }

    public Map<String, Object> getBalance(Long schoolId) {
        SmsCredentials creds = getCredentials(schoolId);
        return darsmsProvider.getBalance(creds.apiKey());
    }

    public List<String> getApprovedSenderIdsByApiKey(String apiKey) {
        return darsmsProvider.getApprovedSenderIds(apiKey);
    }

    public Map<String, Object> getBalanceByApiKey(String apiKey) {
        return darsmsProvider.getBalance(apiKey);
    }

    @Transactional
    public SmsConfigurationDto createConfiguration(CreateSmsConfigurationRequest request) {
        if (request.getSchoolId() == null) {
            throw new BadRequestException("School ID is required");
        }
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        if (smsConfigurationRepository.existsBySchoolIdAndProviderAndDeletedAtIsNull(request.getSchoolId(),
                request.getProvider() != null ? request.getProvider() : "DARSMS")) {
            throw new ConflictException("SMS configuration already exists for this school and provider");
        }

        if (request.getApiKey() == null || request.getApiKey().isBlank()) {
            throw new BadRequestException("API key is required");
        }

        SmsConfiguration config = new SmsConfiguration();
        config.setSchoolId(request.getSchoolId());
        config.setProvider(request.getProvider() != null ? request.getProvider() : "DARSMS");
        config.setEncryptedApiKey(smsEncryptionService.encrypt(request.getApiKey()));
        if (request.getApiSecret() != null && !request.getApiSecret().isBlank()) {
            config.setEncryptedApiSecret(smsEncryptionService.encrypt(request.getApiSecret()));
        }
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
    public SmsConfigurationDto updateConfiguration(Long id, UpdateSmsConfigurationRequest request) {
        var config = smsConfigurationRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("SMS configuration not found"));

        if (request.getProvider() != null) {
            config.setProvider(request.getProvider());
        }
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            config.setEncryptedApiKey(smsEncryptionService.encrypt(request.getApiKey()));
        }
        if (request.getApiSecret() != null && !request.getApiSecret().isBlank()) {
            config.setEncryptedApiSecret(smsEncryptionService.encrypt(request.getApiSecret()));
        }
        if (request.getSenderId() != null) {
            config.setSenderId(request.getSenderId());
        }
        if (request.getMonthlyLimit() != null) {
            config.setMonthlyLimit(request.getMonthlyLimit());
        }
        if (request.getDailyLimit() != null) {
            config.setDailyLimit(request.getDailyLimit());
        }
        if (request.getRemarks() != null) {
            config.setRemarks(request.getRemarks());
        }
        if (request.getIsActive() != null) {
            config.setActive(request.getIsActive());
        }

        var saved = smsConfigurationRepository.save(config);

        auditLogService.log("SMS_CONFIG_UPDATE", "SmsConfiguration", saved.getId(),
                "Updated SMS configuration", null, null, null);

        return mapToDto(saved);
    }

    @Transactional
    public void deleteConfiguration(Long id) {
        var config = smsConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS configuration not found"));

        config.setDeletedAt(LocalDateTime.now());
        smsConfigurationRepository.save(config);

        auditLogService.log("SMS_CONFIG_DELETE", "SmsConfiguration", config.getId(),
                "Deleted SMS configuration", null, null, null);
    }

    /**
     * Validates that a school's SMS configuration can reach DARSMS without sending a
     * real SMS. Uses the documented balance and sender-ids endpoints only.
     * Intended for Super Admin configuration testing only.
     */
    public SmsTestResult testConfiguration(Long schoolId, String recipient) {
        SmsCredentials creds = getCredentials(schoolId);
        if (creds.apiKey == null || creds.apiKey.isBlank()) {
            return new SmsTestResult(false, null, "SMS_NOT_CONFIGURED", null, null);
        }
        try {
            List<String> senderIds = darsmsProvider.getApprovedSenderIds(creds.apiKey());
            Map<String, Object> balance = darsmsProvider.getBalance(creds.apiKey());
            if (recipient != null && !recipient.isBlank()) {
                try {
                    DarsmsProviderService.normalizeTanzanianPhone(recipient);
                } catch (NotificationProviderException e) {
                    return new SmsTestResult(false, null, e.getFailureCode(), null, null);
                }
            }
            return new SmsTestResult(true, null, null, balance, senderIds);
        } catch (NotificationProviderException e) {
            return new SmsTestResult(false, null, e.getFailureCode(), null, null);
        }
    }

    public SmsConfiguration findRawById(Long id) {
        return smsConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMS configuration not found"));
    }

    public SmsConfiguration findRawBySchoolId(Long schoolId) {
        return smsConfigurationRepository.findBySchoolIdAndIsActiveAndDeletedAtIsNull(schoolId, true)
                .orElseThrow(() -> new ResourceNotFoundException("No active SMS configuration found for school"));
    }

    private SmsConfigurationDto mapToDto(SmsConfiguration config) {
        var dto = new SmsConfigurationDto();
        dto.setId(config.getId());
        dto.setUuid(config.getUuid() != null ? config.getUuid().toString() : null);
        dto.setSchoolId(config.getSchoolId());
        schoolRepository.findById(config.getSchoolId()).ifPresent(s -> dto.setSchoolName(s.getName()));
        dto.setProvider(config.getProvider());
        dto.setSenderId(config.getSenderId());
        dto.setMonthlyLimit(config.getMonthlyLimit());
        dto.setDailyLimit(config.getDailyLimit());
        dto.setRemarks(config.getRemarks());
        dto.setActive(config.isActive());
        dto.setCredentialsConfigured(config.getEncryptedApiKey() != null && !config.getEncryptedApiKey().isBlank());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    public record SmsCredentials(String apiKey, String apiSecret, String senderId) {
    }

    public record SmsTestResult(boolean success, String providerMessageId, String failureCode,
                                Map<String, Object> balance, List<String> senderIds) {
    }
}
