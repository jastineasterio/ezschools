package com.edumanage.notifications.controller.platform;

import com.edumanage.exception.GlobalExceptionHandler;
import com.edumanage.notifications.controller.PlatformNotificationController;
import com.edumanage.notifications.dto.NotificationChannelsDto;
import com.edumanage.notifications.dto.TestEmailRequest;
import com.edumanage.notifications.service.CommunicationSettingsService;
import com.edumanage.notifications.service.EmailNotificationProviderService;
import com.edumanage.notifications.service.NotificationProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformNotificationControllerTest {

    private CommunicationSettingsService settingsService;
    private EmailNotificationProviderService emailProvider;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        settingsService = mock(CommunicationSettingsService.class);
        emailProvider = mock(EmailNotificationProviderService.class);
        PlatformNotificationController controller = new PlatformNotificationController(
                settingsService, null, emailProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void configurationReturnsReadinessBooleansAndNoSecrets() throws Exception {
        NotificationChannelsDto dto = new NotificationChannelsDto(true, true, true, true, false);
        when(settingsService.getGlobalSettings()).thenReturn(dto);

        mockMvc.perform(get("/platform/notifications/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationsEnabled").value(true))
                .andExpect(jsonPath("$.data.smsEnabled").value(true))
                .andExpect(jsonPath("$.data.smsConfigured").value(true))
                .andExpect(jsonPath("$.data.emailEnabled").value(true))
                .andExpect(jsonPath("$.data.emailConfigured").value(false));
    }

    @Test
    void testEmailRejectedWhenSmtpNotConfigured() throws Exception {
        doThrow(new NotificationProviderException("EMAIL_NOT_CONFIGURED"))
                .when(emailProvider).send(any(), any(), any());

        mockMvc.perform(post("/platform/notifications/test-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestEmailRequest("admin@example.com", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("EMAIL_NOT_CONFIGURED"));
    }

    @Test
    void testEmailAcceptedWhenSmtpConfigured() throws Exception {
        doNothing().when(emailProvider).send(any(), any(), any());

        mockMvc.perform(post("/platform/notifications/test-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestEmailRequest("admin@example.com", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testEmailRequiresExplicitConfirmation() throws Exception {
        mockMvc.perform(post("/platform/notifications/test-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestEmailRequest("admin@example.com", false))))
                .andExpect(status().isBadRequest());
    }
}
