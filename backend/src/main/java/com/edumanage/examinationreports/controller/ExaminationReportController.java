package com.edumanage.examinationreports.controller;

import com.edumanage.examinationreports.dto.ExaminationReportDto;
import com.edumanage.examinationreports.service.ExaminationReportService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/examination-reports")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ExaminationReportController {

    private final ExaminationReportService examinationReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExaminationReportDto>>> list(Pageable pageable) {
        var reports = examinationReportService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination reports fetched", reports, null, LocalDateTime.now(), "/api/school/examination-reports"));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<PageResponse<ExaminationReportDto>>> listByStudent(@PathVariable Long studentId,
                                                                                        Pageable pageable) {
        var reports = examinationReportService.listByStudent(studentId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination reports fetched by student", reports, null, LocalDateTime.now(), "/api/school/examination-reports/student/" + studentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExaminationReportDto>> getById(@PathVariable Long id) {
        var report = examinationReportService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination report fetched", report, null, LocalDateTime.now(), "/api/school/examination-reports/" + id));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ExaminationReportDto>> generate(@RequestParam Long studentId,
                                                                      @RequestParam Long examinationId) {
        var report = examinationReportService.generate(studentId, examinationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination report generated", report, null, LocalDateTime.now(), "/api/school/examination-reports/generate"));
    }
}