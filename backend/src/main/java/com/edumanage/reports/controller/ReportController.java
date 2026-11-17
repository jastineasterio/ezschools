package com.edumanage.reports.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.reports.dto.CreateReportJobRequest;
import com.edumanage.reports.dto.ReportFilterDefinitionsResponse;
import com.edumanage.reports.dto.ReportJobDto;
import com.edumanage.reports.dto.ReportOptionDto;
import com.edumanage.reports.service.ReportGenerationService;
import com.edumanage.reports.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/reports")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportGenerationService reportGenerationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportJobDto>>> list(Pageable pageable) {
        var reports = reportService.listMyReports(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reports fetched", reports, null, LocalDateTime.now(), "/api/school/reports"));
    }

    @GetMapping("/definitions")
    public ResponseEntity<ApiResponse<ReportFilterDefinitionsResponse>> definitions() {
        var definitions = reportGenerationService.getDefinitions();
        return ResponseEntity.ok(new ApiResponse<>(true, "Report filter definitions", definitions, null, LocalDateTime.now(), "/api/school/reports/definitions"));
    }

    @GetMapping("/options/{kind}")
    public ResponseEntity<ApiResponse<List<ReportOptionDto>>> options(
            @PathVariable String kind,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long departmentId) {
        var options = reportGenerationService.getOptions(kind, classId, academicYearId, departmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report filter options", options, null, LocalDateTime.now(), "/api/school/reports/options/" + kind));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReportJobDto>> create(@Valid @RequestBody CreateReportJobRequest request) {
        var report = reportGenerationService.generate(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report generated", report, null, LocalDateTime.now(), "/api/school/reports"));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<ReportJobDto>> getReport(@PathVariable Long jobId) {
        var report = reportGenerationService.getReport(jobId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report details", report, null, LocalDateTime.now(), "/api/school/reports/" + jobId));
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadPdf(@PathVariable Long jobId) throws Exception {
        return reportGenerationService.downloadPdf(jobId);
    }

    @PostMapping("/{jobId}/regenerate")
    public ResponseEntity<ApiResponse<ReportJobDto>> regenerate(@PathVariable Long jobId) {
        var report = reportGenerationService.regenerate(jobId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report regenerated", report, null, LocalDateTime.now(), "/api/school/reports/" + jobId + "/regenerate"));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long jobId) {
        reportGenerationService.deleteReport(jobId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report deleted", null, null, LocalDateTime.now(), "/api/school/reports/" + jobId));
    }
}
