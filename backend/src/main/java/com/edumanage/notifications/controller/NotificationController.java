package com.edumanage.notifications.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.notifications.dto.CreateNotificationRequest;
import com.edumanage.notifications.dto.NotificationDto;
import com.edumanage.notifications.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> list(Pageable pageable) {
        var notifications = notificationService.listMyNotifications(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notifications fetched", notifications, null, LocalDateTime.now(), "/api/school/notifications"));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> listUnread() {
        var notifications = notificationService.listUnread();
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread notifications fetched", notifications, null, LocalDateTime.now(), "/api/school/notifications/unread"));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> countUnread() {
        var count = notificationService.countUnread();
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread count fetched", count, null, LocalDateTime.now(), "/api/school/notifications/unread/count"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationDto>> create(@Valid @RequestBody CreateNotificationRequest request) {
        Long tenantId = com.edumanage.tenant.TenantContext.getTenantId();
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            userId = ((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id();
        }
        if (userId == null) {
            return ResponseEntity.ok(new ApiResponse<>(false, "User not authenticated", null, null, LocalDateTime.now(), "/api/school/notifications"));
        }

        var notification = notificationService.create(request, userId, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification created", notification, null, LocalDateTime.now(), "/api/school/notifications"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable Long id) {
        var notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification marked as read", notification, null, LocalDateTime.now(), "/api/school/notifications/" + id + "/read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(new ApiResponse<>(true, "All notifications marked as read", null, null, LocalDateTime.now(), "/api/school/notifications/read-all"));
    }
}
