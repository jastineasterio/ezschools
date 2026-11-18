package com.edumanage.notifications.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.notifications.dto.NotificationChannelsDto;
import com.edumanage.notifications.repository.ManagerChannelPermissionRepository;
import com.edumanage.sms.service.SmsConfigurationService;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunicationSettingsServiceTest {

    private JdbcTemplate jdbcTemplate;
    private ManagerChannelPermissionRepository permissionRepository;
    private DarsmsProviderService darsmsProvider;
    private EmailNotificationProviderService emailProvider;
    private SmsConfigurationService smsConfigurationService;
    private AuditLogService auditLogService;
    private CommunicationSettingsService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        permissionRepository = mock(ManagerChannelPermissionRepository.class);
        darsmsProvider = mock(DarsmsProviderService.class);
        emailProvider = mock(EmailNotificationProviderService.class);
        smsConfigurationService = mock(SmsConfigurationService.class);
        auditLogService = mock(AuditLogService.class);
        // Global channel defaults are enabled; readiness is driven by providers.
        when(jdbcTemplate.query(
                any(),
                any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class))).thenReturn(List.of());
        service = new CommunicationSettingsService(
                jdbcTemplate, permissionRepository, mock(UserRepository.class),
                darsmsProvider, emailProvider, smsConfigurationService, auditLogService);
    }

    @Test
    void darsmsReadyWhenGlobalProviderConfigured() {
        when(darsmsProvider.isConfigured()).thenReturn(true);
        when(smsConfigurationService.hasAnyConfiguredSchool()).thenReturn(false);

        NotificationChannelsDto dto = service.getGlobalSettings();

        assertTrue(dto.smsConfigured());
        assertTrue(dto.notificationsEnabled());
        assertTrue(dto.smsEnabled());
    }

    @Test
    void darsmsReadyWhenAnySchoolConfiguredEvenWithoutGlobalCredentials() {
        when(darsmsProvider.isConfigured()).thenReturn(false);
        when(smsConfigurationService.hasAnyConfiguredSchool()).thenReturn(true);

        NotificationChannelsDto dto = service.getGlobalSettings();

        assertTrue(dto.smsConfigured());
    }

    @Test
    void darsmsNotConfiguredWhenNoCredentialsAnywhere() {
        when(darsmsProvider.isConfigured()).thenReturn(false);
        when(smsConfigurationService.hasAnyConfiguredSchool()).thenReturn(false);

        NotificationChannelsDto dto = service.getGlobalSettings();

        assertFalse(dto.smsConfigured());
    }

    @Test
    void emailNotConfiguredWhenSmtpMissing() {
        when(emailProvider.isConfigured()).thenReturn(false);

        NotificationChannelsDto dto = service.getGlobalSettings();

        assertFalse(dto.emailConfigured());
    }

    @Test
    void emailConfiguredWhenSmtpPresent() {
        when(emailProvider.isConfigured()).thenReturn(true);

        NotificationChannelsDto dto = service.getGlobalSettings();

        assertTrue(dto.emailConfigured());
    }

    @Test
    void readinessDtoExposesOnlyBooleansNoSecrets() {
        when(darsmsProvider.isConfigured()).thenReturn(true);
        when(emailProvider.isConfigured()).thenReturn(false);

        NotificationChannelsDto dto = service.getGlobalSettings();

        assertEquals(5, dto.getClass().getDeclaredFields().length);
        boolean allBoolean = java.util.Arrays.stream(dto.getClass().getDeclaredFields())
                .allMatch(field -> field.getType() == boolean.class);
        assertTrue(allBoolean, "NotificationChannelsDto must expose only boolean readiness fields, no secrets");
    }

    @Test
    void managerChannelPermissionsUnchanged() {
        when(smsConfigurationService.isConfigured(eq(10L))).thenReturn(true);
        when(emailProvider.isConfigured()).thenReturn(false);
        // No permission record on file: channel access follows global readiness only.
        when(permissionRepository.findByManagerUserId(1L)).thenReturn(Optional.empty());

        assertTrue(service.isChannelAllowed(1L, 10L, "SMS"));
        assertFalse(service.isChannelAllowed(1L, 10L, "EMAIL"));
        assertEquals("EMAIL provider is not configured", service.disabledReason(1L, 10L, "EMAIL"));
    }
}
