package com.edumanage.results.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.results.dto.ResultAnalyticsDto;
import com.edumanage.results.service.ResultAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/results/analytics")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResultAnalyticsController {

    private final ResultAnalyticsService resultAnalyticsService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HOD')")
    public ResponseEntity<ApiResponse<ResultAnalyticsDto>> schoolAnalytics(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long examinationId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long streamId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ResultAnalyticsDto analytics = resultAnalyticsService.getSchoolAnalytics(
                academicYearId, examinationId, classId, streamId, subjectId, startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(true, "School results analytics fetched", analytics, null,
                LocalDateTime.now(), "/api/school/results/analytics"));
    }

    @GetMapping("/department")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HOD')")
    public ResponseEntity<ApiResponse<ResultAnalyticsDto>> departmentAnalytics(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long examinationId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long streamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ResultAnalyticsDto analytics = resultAnalyticsService.getDepartmentAnalytics(
                academicYearId, examinationId, classId, streamId, startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department results analytics fetched", analytics, null,
                LocalDateTime.now(), "/api/school/results/analytics/department"));
    }
}
