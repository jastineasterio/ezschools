package com.edumanage.reports.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.notifications.service.NotificationService;
import com.edumanage.reports.dto.CreateReportJobRequest;
import com.edumanage.reports.dto.ReportJobDto;
import com.edumanage.reports.entity.ReportJob;
import com.edumanage.reports.repository.ReportJobRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReportService {

    private final ReportJobRepository reportJobRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;

    public ReportService(ReportJobRepository reportJobRepository,
                         AuditLogService auditLogService,
                         NotificationService notificationService,
                         RabbitTemplate rabbitTemplate) {
        this.reportJobRepository = reportJobRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public PageResponse<ReportJobDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<ReportJob> page;
        if (tenantId != null) {
            page = reportJobRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = reportJobRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public PageResponse<ReportJobDto> listMyReports(Pageable pageable) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            userId = ((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id();
        }
        if (userId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return PageResponse.from(reportJobRepository.findBySchoolIdAndRequestedBy(tenantId, userId, pageable).map(this::mapToDto));
        }
        return PageResponse.from(reportJobRepository.findByRequestedBy(userId, pageable).map(this::mapToDto));
    }

    @Transactional
    public ReportJobDto create(CreateReportJobRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.edumanage.security.CustomUserDetails) {
            userId = ((com.edumanage.security.CustomUserDetails) auth.getPrincipal()).id();
        }
        if (userId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        ReportJob job = new ReportJob();
        job.setSchoolId(tenantId);
        job.setRequestedBy(userId);
        job.setReportType(request.getReportType());
        job.setReportName(request.getReportName());
        job.setParameters(request.getParameters());
        job.setStatus("PENDING");
        job.setExpiresAt(LocalDateTime.now().plusDays(7));

        reportJobRepository.save(job);

        try {
            rabbitTemplate.convertAndSend("edumanage.exchange", "reports.generate", job.getId());
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorMessage("Failed to queue report: " + e.getMessage());
            reportJobRepository.save(job);
            throw new RuntimeException("Failed to queue report job", e);
        }

        auditLogService.log("REPORT_CREATE", "ReportJob", job.getId(),
                "Created report job " + job.getReportName(), null, null, null);

        return mapToDto(job);
    }

    @Transactional
    public void updateStatus(Long jobId, String status, String fileUrl, String fileName, Long fileSize, String errorMessage) {
        var job = reportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Report job not found"));

        job.setStatus(status);
        job.setFileUrl(fileUrl);
        job.setFileName(fileName);
        job.setFileSize(fileSize);
        job.setErrorMessage(errorMessage);

        if ("PROCESSING".equals(status)) {
            job.setStartedAt(LocalDateTime.now());
        } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            job.setCompletedAt(LocalDateTime.now());
        }

        reportJobRepository.save(job);
    }

    private ReportJobDto mapToDto(ReportJob entity) {
        ReportJobDto dto = new ReportJobDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setRequestedBy(entity.getRequestedBy());
        dto.setReportType(entity.getReportType());
        dto.setReportName(entity.getReportName());
        dto.setParameters(entity.getParameters());
        dto.setStatus(entity.getStatus());
        dto.setFileUrl(entity.getFileUrl());
        dto.setFileName(entity.getFileName());
        dto.setFileSize(entity.getFileSize());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
