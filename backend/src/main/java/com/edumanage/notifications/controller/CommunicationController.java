package com.edumanage.notifications.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.notifications.dto.CommunicationCampaignDto;
import com.edumanage.notifications.dto.CommunicationDeliveryDto;
import com.edumanage.notifications.dto.CreateCampaignRequest;
import com.edumanage.notifications.dto.NotificationChannelsDto;
import com.edumanage.notifications.dto.RecipientPreviewDto;
import com.edumanage.notifications.service.CommunicationCampaignService;
import com.edumanage.notifications.service.CommunicationSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/communications")
@PreAuthorize("hasAuthority('ROLE_MANAGER')")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationCampaignService campaignService;
    private final CommunicationSettingsService settingsService;

    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<NotificationChannelsDto>> getChannels() {
        var channels = settingsService.getGlobalSettings();
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification channels fetched", channels, null,
                LocalDateTime.now(), "/api/school/communications/channels"));
    }

    @GetMapping("/recipients/preview")
    public ResponseEntity<ApiResponse<RecipientPreviewDto>> preview(
            @RequestParam String group, @RequestParam String channel) {
        var preview = campaignService.preview(group, channel);
        return ResponseEntity.ok(new ApiResponse<>(true, "Recipient preview fetched", preview, null,
                LocalDateTime.now(), "/api/school/communications/recipients/preview"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommunicationCampaignDto>> send(
            @Valid @RequestBody CreateCampaignRequest request) {
        var campaign = campaignService.send(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification campaign processed", campaign, null,
                LocalDateTime.now(), "/api/school/communications"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CommunicationCampaignDto>>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var campaigns = campaignService.list(PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification campaigns fetched", campaigns, null,
                LocalDateTime.now(), "/api/school/communications"));
    }

    @GetMapping("/{id}/deliveries")
    public ResponseEntity<ApiResponse<List<CommunicationDeliveryDto>>> deliveries(@PathVariable Long id) {
        var deliveries = campaignService.getDeliveries(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery attempts fetched", deliveries, null,
                LocalDateTime.now(), "/api/school/communications/" + id + "/deliveries"));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<CommunicationCampaignDto>> retry(@PathVariable Long id) {
        var campaign = campaignService.retryFailed(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Eligible failed deliveries retried", campaign, null,
                LocalDateTime.now(), "/api/school/communications/" + id + "/retry"));
    }
}
