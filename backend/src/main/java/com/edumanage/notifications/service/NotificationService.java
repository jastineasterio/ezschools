package com.edumanage.notifications.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.notifications.dto.CreateNotificationRequest;
import com.edumanage.notifications.dto.NotificationDto;
import com.edumanage.notifications.entity.Notification;
import com.edumanage.notifications.entity.NotificationPreference;
import com.edumanage.notifications.repository.NotificationPreferenceRepository;
import com.edumanage.notifications.repository.NotificationRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final AuditLogService auditLogService;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferenceRepository preferenceRepository,
                               AuditLogService auditLogService) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<NotificationDto> listMyNotifications(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            userId = ((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id();
        }
        if (userId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        Page<Notification> page;
        if (tenantId != null) {
            page = notificationRepository.findByUserIdAndSchoolIdOrderByCreatedAtDesc(userId, tenantId, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<NotificationDto> listUnread() {
        Long tenantId = TenantContext.getTenantId();
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            userId = ((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id();
        }
        if (userId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        Long schoolId = tenantId != null ? tenantId : null;
        if (schoolId != null) {
            return notificationRepository.findByUserIdAndSchoolIdAndIsReadFalseOrderByCreatedAtDesc(userId, schoolId).stream()
                    .map(this::mapToDto)
                    .toList();
        }
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    public long countUnread() {
        Long tenantId = TenantContext.getTenantId();
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            userId = ((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id();
        }
        if (userId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        if (tenantId != null) {
            return notificationRepository.countByUserIdAndSchoolIdAndIsReadFalse(userId, tenantId);
        }
        return 0;
    }

    @Transactional
    public NotificationDto markAsRead(Long id) {
        var notification = notificationRepository.findById(id)
                .filter(n -> !n.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(notification.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        return mapToDto(notification);
    }

    @Transactional
    public void markAllAsRead() {
        Long tenantId = TenantContext.getTenantId();
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            userId = ((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id();
        }
        if (userId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        List<Notification> unread;
        if (tenantId != null) {
            unread = notificationRepository.findByUserIdAndSchoolIdAndIsReadFalseOrderByCreatedAtDesc(userId, tenantId);
        } else {
            unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        }

        for (Notification n : unread) {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public NotificationDto create(CreateNotificationRequest request, Long recipientUserId, Long schoolId) {
        Notification notification = new Notification();
        notification.setSchoolId(schoolId);
        notification.setUserId(recipientUserId);
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setNotificationType(request.getNotificationType() != null ? request.getNotificationType() : "SYSTEM");
        notification.setPriority(request.getPriority() != null ? request.getPriority() : "NORMAL");
        notification.setActionUrl(request.getActionUrl());
        notification.setMetadata(request.getMetadata());

        notificationRepository.save(notification);

        auditLogService.log("NOTIFICATION_CREATE", "Notification", notification.getId(),
                "Created notification for user", null, null, null);

        return mapToDto(notification);
    }

    private NotificationDto mapToDto(Notification entity) {
        NotificationDto dto = new NotificationDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setUserId(entity.getUserId());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setNotificationType(entity.getNotificationType());
        dto.setPriority(entity.getPriority());
        dto.setIsRead(entity.getIsRead());
        dto.setReadAt(entity.getReadAt());
        dto.setActionUrl(entity.getActionUrl());
        dto.setMetadata(entity.getMetadata());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
