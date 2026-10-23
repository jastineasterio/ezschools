package com.edumanage.dashboards.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.dashboards.dto.HodDashboardDto;
import com.edumanage.dashboards.service.HodDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Head of department dashboard endpoint.
 */
@RestController
@RequestMapping("/school/hod")
@PreAuthorize("hasAnyAuthority('ROLE_HOD', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
public class HodDashboardController {

    private final HodDashboardService hodDashboardService;

    public HodDashboardController(HodDashboardService hodDashboardService) {
        this.hodDashboardService = hodDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<HodDashboardDto>> getDashboard() {
        var dashboard = hodDashboardService.getDashboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Head of department dashboard fetched", dashboard, null,
                LocalDateTime.now(), "/api/school/hod/dashboard"));
    }
}
