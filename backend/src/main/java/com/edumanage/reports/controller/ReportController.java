package com.edumanage.reports.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.reports.dto.CreateReportJobRequest;
import com.edumanage.reports.dto.ReportJobDto;
import com.edumanage.reports.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/reports")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportJobDto>>> list(Pageable pageable) {
        var reports = reportService.listMyReports(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reports fetched", reports, null, LocalDateTime.now(), "/api/school/reports"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReportJobDto>> create(@Valid @RequestBody CreateReportJobRequest request) {
        var report = reportService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report job created", report, null, LocalDateTime.now(), "/api/school/reports"));
    }
}
