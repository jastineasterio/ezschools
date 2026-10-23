package com.edumanage.audit.service;

import com.edumanage.audit.dto.AuditLogDto;
import com.edumanage.audit.entity.AuditLog;
import com.edumanage.audit.repository.AuditLogRepository;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.common.util.AuditAware;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String action, String entityType, Long entityId, String description,
                    String ipAddress, String userAgent, String metadata) {
        logForSchool(action, entityType, entityId, description, ipAddress, userAgent, metadata,
                TenantContext.getTenantId());
    }

    @Transactional
    public void logForSchool(String action, String entityType, Long entityId, String description,
                             String ipAddress, String userAgent, String metadata, Long schoolId) {
        AuditLog log = new AuditLog();
        log.setActorUserId(AuditAware.currentUserId());
        log.setSchoolId(schoolId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setMetadata(metadata);
        auditLogRepository.save(log);
    }

    public PageResponse<AuditLogDto> getAuditLogs(Pageable pageable) {
        return getAuditLogs(pageable, null, null, null);
    }

    public PageResponse<AuditLogDto> getAuditLogs(Pageable pageable, String search, String action, String entityType) {
        Long tenantId = TenantContext.getTenantId();
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedAction = action == null || action.isBlank() ? null : action.trim();
        String normalizedEntityType = entityType == null || entityType.isBlank() ? null : entityType.trim();

        Page<AuditLog> page;
        if (tenantId != null) {
            page = auditLogRepository.findFilteredBySchoolId(tenantId, normalizedSearch, normalizedAction, normalizedEntityType, pageable);
        } else {
            page = auditLogRepository.findFiltered(normalizedSearch, normalizedAction, normalizedEntityType, pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public PageResponse<AuditLogDto> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    private AuditLogDto mapToDto(AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(log.getId());
        dto.setActorUserId(log.getActorUserId());
        dto.setSchoolId(log.getSchoolId());
        dto.setAction(log.getAction());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setDescription(log.getDescription());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setMetadata(log.getMetadata());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
