package com.edumanage.platform.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.platform.dto.PlatformSettingsDto;
import com.edumanage.platform.service.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/platform/settings")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformSettingsController {

    private final PlatformSettingsService platformSettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformSettingsDto>> getSettings() {
        var settings = platformSettingsService.getSettings();
        return ResponseEntity.ok(new ApiResponse<>(true, "Settings fetched", settings, null,
                LocalDateTime.now(), "/api/platform/settings"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PlatformSettingsDto>> updateSettings(@RequestBody Map<String, Object> updates) {
        var settings = platformSettingsService.updateSettings(updates);
        return ResponseEntity.ok(new ApiResponse<>(true, "Settings updated", settings, null,
                LocalDateTime.now(), "/api/platform/settings"));
    }
}
