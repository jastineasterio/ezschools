package com.edumanage.notifications.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.notifications.dto.RecipientPreviewDto;import com.edumanage.sms.service.SmsConfigurationService;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommunicationCampaignServiceTest {

    private CommunicationRecipientService recipientService;
    private CommunicationSettingsService settingsService;
    private SmsConfigurationService smsConfigurationService;
    private UserRepository userRepository;
    private CommunicationCampaignService service;

    private final User manager = new User();

    @BeforeEach
    void setUp() {
        recipientService = mock(CommunicationRecipientService.class);
        settingsService = mock(CommunicationSettingsService.class);
        smsConfigurationService = mock(SmsConfigurationService.class);
        userRepository = mock(UserRepository.class);

        manager.setId(10L);
        manager.setEmail("manager@school.com");
        manager.setSchoolId(1L);
        manager.setActive(true);

        service = new CommunicationCampaignService(
                mock(com.edumanage.notifications.repository.CommunicationCampaignRepository.class),
                mock(com.edumanage.notifications.repository.CommunicationDeliveryRepository.class),
                recipientService, settingsService,
                mock(DarsmsProviderService.class),
                mock(EmailNotificationProviderService.class),
                smsConfigurationService, userRepository,
                mock(AuditLogService.class));

        TenantContext.clear();
    }

    private void authAsManager() {
        var auth = new UsernamePasswordAuthenticationToken(
                new com.edumanage.security.CustomUserDetails(10L, "manager@school.com", "pass", 1L, true, List.of()),
                null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(1L);
        when(userRepository.findByEmail("manager@school.com")).thenReturn(Optional.of(manager));
    }

    private CommunicationRecipient recipient(String phone) {
        return new CommunicationRecipient("USER", 100L, "John Doe", "john@x.com", phone);
    }

    @Test
    void previewReturnsExcludedCountForInvalidPhone() {
        var recipient = recipient("0712345678");
        when(recipientService.resolve(1L, "STUDENTS", null)).thenReturn(List.of(recipient));
        when(recipientService.hasEligibleContact(recipient, "SMS")).thenReturn(false);
        when(settingsService.isChannelAllowed(10L, 1L, "SMS")).thenReturn(true);

        authAsManager();
        RecipientPreviewDto preview = service.preview("STUDENTS", "SMS");
        assertEquals(0, preview.eligibleCount());
        assertEquals(1, preview.excludedCount());
        TenantContext.clear();
    }

    @Test
    void previewReturnsEligibleCountForValidPhone() {
        var recipient = recipient("0712345678");
        when(recipientService.resolve(1L, "STUDENTS", null)).thenReturn(List.of(recipient));
        when(recipientService.hasEligibleContact(recipient, "SMS")).thenReturn(true);
        when(settingsService.isChannelAllowed(10L, 1L, "SMS")).thenReturn(true);

        authAsManager();
        RecipientPreviewDto preview = service.preview("STUDENTS", "SMS");
        assertEquals(1, preview.eligibleCount());
        assertEquals(0, preview.excludedCount());
        TenantContext.clear();
    }

    @Test
    void uncertainProviderOutcomeCodesAreNotPermanent() {
        List<String> permanent = List.of(
                "SMS_NOT_CONFIGURED", "SMS_PROVIDER_AUTH_FAILED",
                "SMS_PROVIDER_ACCOUNT_DISABLED", "SMS_PROVIDER_REJECTED_RECIPIENT",
                "SMS_PROVIDER_REJECTED", "SMS_PROVIDER_INVALID_RESPONSE",
                "INVALID_PHONE", "RECIPIENT_NO_LONGER_AVAILABLE");
        List<String> uncertain = List.of(
                "SMS_PROVIDER_UNAVAILABLE", "SMS_PROVIDER_CONNECTION_FAILED",
                "SMS_PROVIDER_CONNECT_TIMEOUT", "SMS_PROVIDER_READ_TIMEOUT",
                "SMS_PROVIDER_TLS_FAILURE", "SMS_PROVIDER_SERVER_ERROR",
                "SMS_PROVIDER_RATE_LIMITED");

        assertTrue(uncertain.contains("SMS_PROVIDER_READ_TIMEOUT"));
        assertFalse(permanent.contains("SMS_PROVIDER_READ_TIMEOUT"));
        assertTrue(permanent.contains("SMS_PROVIDER_REJECTED"));
        assertFalse(uncertain.contains("SMS_PROVIDER_REJECTED"));
    }

    @Test
    void deliveryDtoCarriesNewFields() {
        com.edumanage.notifications.dto.CommunicationDeliveryDto dto =
                new com.edumanage.notifications.dto.CommunicationDeliveryDto(
                        1L, "USER", 100L, "SMS", "SENT", "msg-123", null, null,
                        "SENT", null, null, "batch-1", 1, null, "John Doe", "0712345678");
        assertEquals("SENT", dto.status());
        assertEquals("msg-123", dto.providerMessageId());
        assertEquals("John Doe", dto.recipientName());
        assertEquals("0712345678", dto.recipientPhone());
        assertNull(dto.failureCode());
        TenantContext.clear();
    }

    @Test
    void campaignDtoCarriesDeliveredCount() {
        com.edumanage.notifications.dto.CommunicationCampaignDto dto =
                new com.edumanage.notifications.dto.CommunicationCampaignDto(
                        1L, 1L, 10L, "Title", "GENERAL", "SMS", "STUDENTS",
                        5, 1, 3, 1, 1, "PARTIALLY SENT", null);
        assertEquals(5, dto.intendedCount());
        assertEquals(1, dto.excludedCount());
        assertEquals(3, dto.sentCount());
        assertEquals(1, dto.deliveredCount());
        assertEquals(1, dto.failedCount());
        assertEquals("PARTIALLY SENT", dto.status());
        TenantContext.clear();
    }
}
