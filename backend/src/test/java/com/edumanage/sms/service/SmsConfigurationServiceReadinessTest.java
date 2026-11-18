package com.edumanage.sms.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.notifications.service.DarsmsProviderService;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.sms.entity.SmsConfiguration;
import com.edumanage.sms.repository.SmsConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmsConfigurationServiceReadinessTest {

    private SmsConfigurationRepository repository;
    private com.edumanage.sms.service.SmsConfigurationService service;

    @BeforeEach
    void setUp() {
        repository = mock(SmsConfigurationRepository.class);
        service = new com.edumanage.sms.service.SmsConfigurationService(
                repository, mock(com.edumanage.sms.service.SmsEncryptionService.class),
                mock(AuditLogService.class), mock(SchoolRepository.class), mock(DarsmsProviderService.class));
    }

    private SmsConfiguration config(boolean active, LocalDateTime deletedAt, String encryptedApiKey, String senderId) {
        SmsConfiguration c = new SmsConfiguration();
        c.setId(1L);
        c.setSchoolId(10L);
        c.setProvider("DARSMS");
        c.setEncryptedApiKey(encryptedApiKey);
        c.setSenderId(senderId);
        c.setActive(active);
        c.setDeletedAt(deletedAt);
        return c;
    }

    @Test
    void isConfiguredWhenSchoolHasActiveConfigWithKeyAndSender() {
        when(repository.findBySchoolIdAndIsActiveAndDeletedAtIsNull(eq(10L), eq(true)))
                .thenReturn(Optional.of(config(true, null, "encrypted-key", "SENDER")));

        assertTrue(service.isConfigured(10L));
    }

    @Test
    void isNotConfiguredWhenSchoolHasNoActiveConfig() {
        when(repository.findBySchoolIdAndIsActiveAndDeletedAtIsNull(eq(10L), eq(true)))
                .thenReturn(Optional.empty());

        assertFalse(service.isConfigured(10L));
    }

    @Test
    void isNotConfiguredWhenApiKeyBlank() {
        when(repository.findBySchoolIdAndIsActiveAndDeletedAtIsNull(eq(10L), eq(true)))
                .thenReturn(Optional.of(config(true, null, "", "SENDER")));

        assertFalse(service.isConfigured(10L));
    }

    @Test
    void isNotConfiguredWhenSenderIdBlank() {
        when(repository.findBySchoolIdAndIsActiveAndDeletedAtIsNull(eq(10L), eq(true)))
                .thenReturn(Optional.of(config(true, null, "encrypted-key", "")));

        assertFalse(service.isConfigured(10L));
    }

    @Test
    void hasAnyConfiguredSchoolTrueWhenAnySchoolConfigured() {
        when(repository.findByDeletedAtIsNull(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(config(true, null, "encrypted-key", "SENDER"))));

        assertTrue(service.hasAnyConfiguredSchool());
    }

    @Test
    void hasAnyConfiguredSchoolFalseWhenNoneConfigured() {
        when(repository.findByDeletedAtIsNull(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(config(true, null, "", ""))));

        assertFalse(service.hasAnyConfiguredSchool());
    }

    @Test
    void hasAnyConfiguredSchoolIgnoresInactiveConfigs() {
        // The repository excludes deleted_at-is-not-null rows; the service must still
        // ignore inactive (isActive=false) configurations.
        when(repository.findByDeletedAtIsNull(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(config(false, null, "encrypted-key", "SENDER"))));

        assertFalse(service.hasAnyConfiguredSchool());
    }
}
