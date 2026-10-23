package com.edumanage.dashboards.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.dashboards.dto.LibrarianDashboardDto;
import com.edumanage.dashboards.service.LibrarianDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Librarian dashboard endpoint. Figures are limited to the library of the
 * authenticated librarian's own school.
 */
@RestController
@RequestMapping("/school/librarian")
@PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
public class LibrarianDashboardController {

    private final LibrarianDashboardService librarianDashboardService;

    public LibrarianDashboardController(LibrarianDashboardService librarianDashboardService) {
        this.librarianDashboardService = librarianDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<LibrarianDashboardDto>> getDashboard() {
        var dashboard = librarianDashboardService.getDashboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Librarian dashboard fetched", dashboard, null,
                LocalDateTime.now(), "/api/school/librarian/dashboard"));
    }
}
