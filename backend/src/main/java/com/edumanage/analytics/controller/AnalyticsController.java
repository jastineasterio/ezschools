package com.edumanage.analytics.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.analytics.dto.AnalyticsSnapshotDto;
import com.edumanage.analytics.dto.CreateAnalyticsSnapshotRequest;
import com.edumanage.analytics.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/platform/analytics")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AnalyticsSnapshotDto>>> list(Pageable pageable) {
        var snapshots = analyticsService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Analytics fetched", snapshots, null, LocalDateTime.now(), "/api/platform/analytics"));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<AnalyticsSnapshotDto>>> listByDateRange(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        var snapshots = analyticsService.listByDateRange(start, end);
        return ResponseEntity.ok(new ApiResponse<>(true, "Analytics fetched", snapshots, null, LocalDateTime.now(), "/api/platform/analytics/range"));
    }

    @GetMapping("/metric/{metricType}")
    public ResponseEntity<ApiResponse<List<AnalyticsSnapshotDto>>> listByMetricType(@PathVariable String metricType) {
        var snapshots = analyticsService.listByMetricType(metricType);
        return ResponseEntity.ok(new ApiResponse<>(true, "Analytics fetched", snapshots, null, LocalDateTime.now(), "/api/platform/analytics/metric/" + metricType));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnalyticsSnapshotDto>> create(@Valid @RequestBody CreateAnalyticsSnapshotRequest request) {
        var snapshot = analyticsService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Analytics snapshot created", snapshot, null, LocalDateTime.now(), "/api/platform/analytics"));
    }
}
