package com.edumanage.notifications.controller.platform;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.notifications.dto.global.*;
import com.edumanage.notifications.dto.settings.NotificationSettingsDto;
import com.edumanage.notifications.dto.settings.TestNotificationRequest;
import com.edumanage.notifications.dto.settings.UpdateNotificationSettingsRequest;
import com.edumanage.notifications.service.management.NotificationManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/notifications")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformNotificationManagementController {

    private final NotificationManagementService notificationManagementService;

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingsDto>> getSettings() {
        var settings = notificationManagementService.getSettings();
        return ResponseEntity.ok(new ApiResponse<>(true, "Settings fetched", settings, null,
                LocalDateTime.now(), "/api/platform/notifications/settings"));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingsDto>> updateSettings(
            @RequestBody UpdateNotificationSettingsRequest request) {
        var settings = notificationManagementService.updateSettings(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Settings updated", settings, null,
                LocalDateTime.now(), "/api/platform/notifications/settings"));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Void>> sendTest(@RequestBody TestNotificationRequest request) {
        notificationManagementService.sendTestNotification(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Test notification sent", null, null,
                LocalDateTime.now(), "/api/platform/notifications/test"));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<PageResponse<NotificationTemplateDto>>> listTemplates(Pageable pageable) {
        var templates = notificationManagementService.listTemplates(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Templates fetched", templates, null,
                LocalDateTime.now(), "/api/platform/notifications/templates"));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<NotificationTemplateDto>> createTemplate(
            @RequestBody CreateNotificationTemplateRequest request) {
        var template = notificationManagementService.createTemplate(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Template created", template, null,
                LocalDateTime.now(), "/api/platform/notifications/templates"));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<NotificationTemplateDto>> updateTemplate(
            @PathVariable Long id, @RequestBody CreateNotificationTemplateRequest request) {
        var template = notificationManagementService.updateTemplate(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Template updated", template, null,
                LocalDateTime.now(), "/api/platform/notifications/templates/" + id));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        notificationManagementService.deleteTemplate(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Template deleted", null, null,
                LocalDateTime.now(), "/api/platform/notifications/templates/" + id));
    }

    @GetMapping("/history/logs")
    public ResponseEntity<ApiResponse<PageResponse<NotificationHistoryDto>>> listHistory(Pageable pageable) {
        var history = notificationManagementService.listHistory(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "History fetched", history, null,
                LocalDateTime.now(), "/api/platform/notifications/history/logs"));
    }
}
