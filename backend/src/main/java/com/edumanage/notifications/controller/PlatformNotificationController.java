package com.edumanage.notifications.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.notifications.dto.CommunicationCampaignDto;
import com.edumanage.notifications.dto.CommunicationDeliveryDto;
import com.edumanage.notifications.dto.ManagerChannelPermissionDto;
import com.edumanage.notifications.dto.NotificationChannelsDto;
import com.edumanage.notifications.dto.TestEmailRequest;
import com.edumanage.notifications.dto.UpdateManagerChannelsRequest;
import com.edumanage.notifications.dto.UpdateNotificationChannelsRequest;
import com.edumanage.notifications.service.CommunicationCampaignService;
import com.edumanage.notifications.service.CommunicationSettingsService;
import com.edumanage.notifications.service.EmailNotificationProviderService;
import com.edumanage.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/platform/notifications")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformNotificationController {

    private final CommunicationSettingsService settingsService;
    private final CommunicationCampaignService campaignService;
    private final EmailNotificationProviderService emailProvider;

    @GetMapping("/configuration")
    public ResponseEntity<ApiResponse<NotificationChannelsDto>> configuration() {
        return response(settingsService.getGlobalSettings(), "Notification configuration fetched",
                "/api/platform/notifications/configuration");
    }

    @PutMapping("/configuration")
    public ResponseEntity<ApiResponse<NotificationChannelsDto>> updateConfiguration(
            @Valid @RequestBody UpdateNotificationChannelsRequest request) {
        var result = settingsService.updateGlobalSettings(request.notificationsEnabled(),
                request.smsEnabled(), request.emailEnabled());
        return response(result, "Notification configuration updated",
                "/api/platform/notifications/configuration");
    }

    @GetMapping("/managers")
    public ResponseEntity<ApiResponse<List<ManagerChannelPermissionDto>>> managers() {
        return response(settingsService.listManagers(), "Manager notification permissions fetched",
                "/api/platform/notifications/managers");
    }

    @PutMapping("/managers/{id}/channels")
    public ResponseEntity<ApiResponse<ManagerChannelPermissionDto>> updateManager(
            @PathVariable Long id, @Valid @RequestBody UpdateManagerChannelsRequest request) {
        var permission = settingsService.updateManager(id, request.smsEnabled(), request.emailEnabled());
        return response(permission, "Manager notification permissions updated",
                "/api/platform/notifications/managers/" + id + "/channels");
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<CommunicationCampaignDto>>> history(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var campaigns = campaignService.listAllForPlatform(PageRequest.of(page, Math.min(size, 100)));
        return response(campaigns, "Notification history fetched", "/api/platform/notifications/history");
    }

    @GetMapping("/history/{id}/deliveries")
    public ResponseEntity<ApiResponse<List<CommunicationDeliveryDto>>> deliveries(@PathVariable Long id) {
        return response(campaignService.getDeliveriesForPlatform(id), "Delivery attempts fetched",
                "/api/platform/notifications/history/" + id + "/deliveries");
    }

    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse<Void>> testEmail(@Valid @RequestBody TestEmailRequest request) {
        if (!request.confirmSend()) {
            throw new BadRequestException("Explicit confirmation is required to send a test email");
        }
        emailProvider.send(request.recipient(), "EduManage email configuration test",
                "This is a test email requested by a Super Admin. No school data is included.");
        return response(null, "Test email accepted by the configured SMTP server",
                "/api/platform/notifications/test-email");
    }

    private <T> ResponseEntity<ApiResponse<T>> response(T data, String message, String path) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, data, null, LocalDateTime.now(), path));
    }
}
