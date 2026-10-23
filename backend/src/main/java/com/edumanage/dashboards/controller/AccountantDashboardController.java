package com.edumanage.dashboards.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.dashboards.dto.AccountantDashboardDto;
import com.edumanage.dashboards.service.AccountantDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Accountant dashboard endpoint. Only finance roles may read the figures and
 * the figures always belong to the school of the authenticated accountant.
 */
@RestController
@RequestMapping("/school/accountant")
@PreAuthorize("hasAnyAuthority('ROLE_ACCOUNTANT', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
public class AccountantDashboardController {

    private final AccountantDashboardService accountantDashboardService;

    public AccountantDashboardController(AccountantDashboardService accountantDashboardService) {
        this.accountantDashboardService = accountantDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AccountantDashboardDto>> getDashboard() {
        var dashboard = accountantDashboardService.getDashboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Accountant dashboard fetched", dashboard, null,
                LocalDateTime.now(), "/api/school/accountant/dashboard"));
    }
}
