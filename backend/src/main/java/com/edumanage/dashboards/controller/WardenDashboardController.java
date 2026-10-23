package com.edumanage.dashboards.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.dashboards.dto.WardenDashboardDto;
import com.edumanage.dashboards.service.WardenDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Boarding warden dashboard endpoint.
 */
@RestController
@RequestMapping("/school/warden")
@PreAuthorize("hasAnyAuthority('ROLE_WARDEN', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
public class WardenDashboardController {

    private final WardenDashboardService wardenDashboardService;

    public WardenDashboardController(WardenDashboardService wardenDashboardService) {
        this.wardenDashboardService = wardenDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<WardenDashboardDto>> getDashboard() {
        var dashboard = wardenDashboardService.getDashboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Warden dashboard fetched", dashboard, null,
                LocalDateTime.now(), "/api/school/warden/dashboard"));
    }
}
