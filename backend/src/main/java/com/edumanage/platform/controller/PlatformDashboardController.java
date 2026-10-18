package com.edumanage.platform.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.platform.dto.PlatformDashboardDto;
import com.edumanage.platform.service.PlatformDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/dashboard")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformDashboardController {

    private final PlatformDashboardService platformDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformDashboardDto>> getDashboard() {
        var dashboard = platformDashboardService.getDashboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard fetched", dashboard, null, LocalDateTime.now(), "/api/platform/dashboard"));
    }
}
