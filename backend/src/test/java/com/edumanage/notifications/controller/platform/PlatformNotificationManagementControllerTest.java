package com.edumanage.notifications.controller.platform;

import com.edumanage.common.dto.PageResponse;
import com.edumanage.notifications.dto.global.NotificationHistoryDto;
import com.edumanage.notifications.service.management.NotificationManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PlatformNotificationManagementControllerTest {

    private NotificationManagementService notificationManagementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationManagementService = mock(NotificationManagementService.class);
        mockMvc = standaloneSetup(new PlatformNotificationManagementController(notificationManagementService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listHistoryReturnsPagedNotificationHistoryAtLogsRoute() throws Exception {
        var sentAt = LocalDateTime.parse("2026-10-17T09:00:00");
        var entry = new NotificationHistoryDto(
                42L, "history-uuid", "TEST_NOTIFICATION", "EMAIL", "EMAIL", "admin@example.com",
                "Test Notification", "Test message", "SENT", "SMTP", "provider-123", null,
                sentAt, sentAt);
        var page = new PageResponse<>(List.of(entry), 1, 1, 2, 2, false);
        when(notificationManagementService.listHistory(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/platform/notifications/history/logs")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageNumber").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(42))
                .andExpect(jsonPath("$.data.content[0].eventCode").value("TEST_NOTIFICATION"))
                .andExpect(jsonPath("$.data.content[0].status").value("SENT"))
                .andExpect(jsonPath("$.path").value("/api/platform/notifications/history/logs"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationManagementService).listHistory(pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(1, pageable.getValue().getPageSize());
    }
}
