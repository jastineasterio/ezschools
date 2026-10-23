package com.edumanage.dashboards.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.dashboards.dto.ParentDashboardDto;
import com.edumanage.dashboards.service.ParentDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Parent dashboard endpoint. The payload is derived from the authenticated
 * parent account, so a parent can never request another family's children.
 */
@RestController
@RequestMapping("/school/parent")
@PreAuthorize("hasAnyAuthority('ROLE_PARENT', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
public class ParentDashboardController {

    private final ParentDashboardService parentDashboardService;

    public ParentDashboardController(ParentDashboardService parentDashboardService) {
        this.parentDashboardService = parentDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ParentDashboardDto>> getDashboard() {
        var dashboard = parentDashboardService.getDashboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Parent dashboard fetched", dashboard, null,
                LocalDateTime.now(), "/api/school/parent/dashboard"));
    }
}
