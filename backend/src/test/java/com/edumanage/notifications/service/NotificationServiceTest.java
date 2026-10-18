package com.edumanage.notifications.service;

import com.edumanage.notifications.entity.Notification;
import com.edumanage.notifications.entity.NotificationPreference;
import com.edumanage.notifications.repository.NotificationPreferenceRepository;
import com.edumanage.notifications.repository.NotificationRepository;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationPreferenceRepository preferenceRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        preferenceRepository = mock(NotificationPreferenceRepository.class);
        notificationService = new NotificationService(notificationRepository, preferenceRepository, mock(com.edumanage.audit.service.AuditLogService.class));
        SecurityContextHolder.clearContext();
    }

    @Test
    void testListMyNotificationsTenantScoped() {
        TenantContext.setTenantId(1L);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new com.edumanage.security.CustomUserDetails(1L, "user@test.com", "pass", 1L, true, List.of()),
                null,
                List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Page<Notification> page = Page.empty();
        when(notificationRepository.findByUserIdAndSchoolIdOrderByCreatedAtDesc(1L, 1L, org.springframework.data.domain.Pageable.unpaged())).thenReturn(page);

        var result = notificationService.listMyNotifications(org.springframework.data.domain.Pageable.unpaged());
        assertNotNull(result);
        verify(notificationRepository).findByUserIdAndSchoolIdOrderByCreatedAtDesc(1L, 1L, org.springframework.data.domain.Pageable.unpaged());
        TenantContext.clear();
    }
}
