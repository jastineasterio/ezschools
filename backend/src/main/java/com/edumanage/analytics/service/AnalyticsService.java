package com.edumanage.analytics.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.analytics.dto.AnalyticsSnapshotDto;
import com.edumanage.analytics.dto.CreateAnalyticsSnapshotRequest;
import com.edumanage.analytics.entity.AnalyticsSnapshot;
import com.edumanage.analytics.repository.AnalyticsSnapshotRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsService {

    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final AuditLogService auditLogService;

    public AnalyticsService(AnalyticsSnapshotRepository analyticsSnapshotRepository, AuditLogService auditLogService) {
        this.analyticsSnapshotRepository = analyticsSnapshotRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<AnalyticsSnapshotDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<AnalyticsSnapshot> page;
        if (tenantId != null) {
            page = analyticsSnapshotRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = analyticsSnapshotRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<AnalyticsSnapshotDto> listByDateRange(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return analyticsSnapshotRepository.findBySchoolIdAndSnapshotDateBetween(tenantId, start, end).stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<AnalyticsSnapshotDto> listByMetricType(String metricType) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return analyticsSnapshotRepository.findBySchoolIdAndMetricType(tenantId, metricType).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public AnalyticsSnapshotDto create(CreateAnalyticsSnapshotRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            userId = ((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id();
        }

        AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
        snapshot.setSchoolId(tenantId);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setMetricType(request.getMetricType());
        snapshot.setMetricValue(request.getMetricValue());
        snapshot.setCreatedBy(userId);
        snapshot.setUpdatedBy(userId);

        analyticsSnapshotRepository.save(snapshot);

        auditLogService.log("ANALYTICS_CREATE", "AnalyticsSnapshot", snapshot.getId(),
                "Created analytics snapshot for " + snapshot.getMetricType(), null, null, null);

        return mapToDto(snapshot);
    }

    private AnalyticsSnapshotDto mapToDto(AnalyticsSnapshot entity) {
        AnalyticsSnapshotDto dto = new AnalyticsSnapshotDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setSnapshotDate(entity.getSnapshotDate());
        dto.setMetricType(entity.getMetricType());
        dto.setMetricValue(entity.getMetricValue());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
