package com.edumanage.audit.controller;

import com.edumanage.audit.dto.AuditChartPoint;
import com.edumanage.audit.dto.AuditDistributionItem;
import com.edumanage.audit.dto.AuditLogDto;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.audit.service.AuditLogService.AuditOverview;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/platform/audit-logs")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long actor,
            @RequestParam(required = false) Long school) {
        PageResponse<AuditLogDto> logs = auditLogService.getAuditLogs(
                sanitize(pageable), search, action, entityType, start, end, result, actor, school);
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs fetched", logs, null,
                java.time.LocalDateTime.now(), "/api/platform/audit-logs"));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AuditOverview>> auditOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long actor,
            @RequestParam(required = false) Long school) {
        LocalDateTime effectiveStart = start != null ? start : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveEnd = end != null ? end : LocalDateTime.now();
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit log overview",
                auditLogService.getOverview(effectiveStart, effectiveEnd, search, action, entityType,
                        result, actor, school), null, java.time.LocalDateTime.now(),
                "/api/platform/audit-logs/overview"));
    }

    @GetMapping("/charts/trend")
    public ResponseEntity<ApiResponse<List<AuditChartPoint>>> auditTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long actor,
            @RequestParam(required = false) Long school) {
        LocalDateTime effectiveStart = start != null ? start : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveEnd = end != null ? end : LocalDateTime.now();
        List<AuditChartPoint> trend = auditLogService.getActivityTrend(
                effectiveStart, effectiveEnd, search, action, entityType, result, actor, school);
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit activity trend", trend, null,
                java.time.LocalDateTime.now(), "/api/platform/audit-logs/charts/trend"));
    }

    @GetMapping("/charts/actions")
    public ResponseEntity<ApiResponse<List<AuditDistributionItem>>> auditActions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long actor,
            @RequestParam(required = false) Long school) {
        LocalDateTime effectiveStart = start != null ? start : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveEnd = end != null ? end : LocalDateTime.now();
        List<AuditDistributionItem> distribution = auditLogService.getActionDistribution(
                effectiveStart, effectiveEnd, search, action, entityType, result, actor, school);
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit action distribution", distribution, null,
                java.time.LocalDateTime.now(), "/api/platform/audit-logs/charts/actions"));
    }

    @GetMapping("/charts/results")
    public ResponseEntity<ApiResponse<List<AuditDistributionItem>>> auditResults(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long actor,
            @RequestParam(required = false) Long school) {
        LocalDateTime effectiveStart = start != null ? start : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveEnd = end != null ? end : LocalDateTime.now();
        List<AuditDistributionItem> distribution = auditLogService.getResultDistribution(
                effectiveStart, effectiveEnd, search, action, entityType, result, actor, school);
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit result distribution", distribution, null,
                java.time.LocalDateTime.now(), "/api/platform/audit-logs/charts/results"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogDto>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit log fetched",
                auditLogService.getAuditLog(id), null, java.time.LocalDateTime.now(),
                "/api/platform/audit-logs/" + id));
    }

    private Pageable sanitize(Pageable pageable) {
        if (!pageable.isPaged()) {
            return PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt");
        }
        Set<String> allowed = Set.of("id", "createdAt", "action", "result", "actorUserId", "schoolId", "entityType");
        boolean valid = pageable.getSort().stream().allMatch(order -> allowed.contains(order.getProperty()));
        if (!valid || pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.Direction.DESC, "createdAt");
        }
        return pageable;
    }
}
