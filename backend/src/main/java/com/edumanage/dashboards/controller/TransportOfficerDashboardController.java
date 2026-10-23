package com.edumanage.dashboards.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.dashboards.dto.TransportOfficerDashboardDto;
import com.edumanage.dashboards.service.TransportOfficerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Transport officer dashboard endpoint.
 */
@RestController
@RequestMapping("/school/transport-officer")
@PreAuthorize("hasAnyAuthority('ROLE_TRANSPORT_OFFICER', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
public class TransportOfficerDashboardController {

    private final TransportOfficerDashboardService transportOfficerDashboardService;

    public TransportOfficerDashboardController(TransportOfficerDashboardService transportOfficerDashboardService) {
        this.transportOfficerDashboardService = transportOfficerDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<TransportOfficerDashboardDto>> getDashboard() {
        var dashboard = transportOfficerDashboardService.getDashboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport officer dashboard fetched", dashboard, null,
                LocalDateTime.now(), "/api/school/transport-officer/dashboard"));
    }
}
