package com.edumanage.platform.analytics.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.platform.analytics.dto.ActiveInactiveDto;
import com.edumanage.platform.analytics.dto.PackageUserDistributionDto;
import com.edumanage.platform.analytics.dto.RoleDistributionDto;
import com.edumanage.platform.analytics.dto.SchoolUserDistributionDto;
import com.edumanage.platform.analytics.dto.TrendDto;
import com.edumanage.platform.analytics.dto.UserOverviewDto;
import com.edumanage.platform.analytics.service.PlatformUserAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/platform/analytics/users")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformUserAnalyticsController {

    private final PlatformUserAnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<UserOverviewDto>> overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        UserOverviewDto data = analyticsService.getOverview(start, end);
        return ResponseEntity.ok(new ApiResponse<>(true, "User analytics overview", data, null,
                LocalDateTime.now(), "/api/platform/analytics/users/overview"));
    }

    @GetMapping("/schools")
    public ResponseEntity<ApiResponse<Page<SchoolUserDistributionDto>>> schools(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "totalUsers", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SchoolUserDistributionDto> data = analyticsService.getSchoolDistribution(start, end, search, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "School user distribution", data, null,
                LocalDateTime.now(), "/api/platform/analytics/users/schools"));
    }

    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<PackageUserDistributionDto>>> packages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<PackageUserDistributionDto> data = analyticsService.getPackageDistribution(start, end);
        return ResponseEntity.ok(new ApiResponse<>(true, "Package user distribution", data, null,
                LocalDateTime.now(), "/api/platform/analytics/users/packages"));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleDistributionDto>>> roles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        UserOverviewDto overview = analyticsService.getOverview(start, end);
        List<RoleDistributionDto> data = overview.getRoleDistribution();
        return ResponseEntity.ok(new ApiResponse<>(true, "Role distribution", data, null,
                LocalDateTime.now(), "/api/platform/analytics/users/roles"));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<ActiveInactiveDto>>> status(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<ActiveInactiveDto> data = analyticsService.getStatusDistribution(start, end);
        return ResponseEntity.ok(new ApiResponse<>(true, "Active vs inactive distribution", data, null,
                LocalDateTime.now(), "/api/platform/analytics/users/status"));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<TrendDto>>> trends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "day") String granularity) {
        List<TrendDto> data = analyticsService.getTrends(start, end, granularity);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registration trends", data, null,
                LocalDateTime.now(), "/api/platform/analytics/users/trends"));
    }
}
