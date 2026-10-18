package com.edumanage.audit.controller;

import com.edumanage.audit.dto.AuditLogDto;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/audit-logs")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAuditLogs(Pageable pageable) {
        var logs = auditLogService.getAuditLogs(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs fetched", logs, null, LocalDateTime.now(), "/api/platform/audit-logs"));
    }
}
