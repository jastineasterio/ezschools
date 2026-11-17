package com.edumanage.examinationtypes.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.examinationtypes.dto.CreateExaminationTypeRequest;
import com.edumanage.examinationtypes.dto.ExaminationTypeDto;
import com.edumanage.examinationtypes.entity.ExaminationType;
import com.edumanage.examinationtypes.repository.ExaminationTypeRepository;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ExaminationTypeService {

    private final ExaminationTypeRepository examinationTypeRepository;
    private final ExaminationRepository examinationRepository;
    private final AuditLogService auditLogService;

    public ExaminationTypeService(ExaminationTypeRepository examinationTypeRepository,
                                  ExaminationRepository examinationRepository,
                                  AuditLogService auditLogService) {
        this.examinationTypeRepository = examinationTypeRepository;
        this.examinationRepository = examinationRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<ExaminationTypeDto> list(Pageable pageable, String search, Boolean active) {
        Long tenantId = TenantContext.getTenantId();
        Page<ExaminationType> page;
        if (tenantId != null) {
            boolean activeOnly = Boolean.TRUE.equals(active);
            if (search != null && !search.isBlank()) {
                page = examinationTypeRepository.findBySchoolIdAndDeletedAtIsNullAndNameContainingIgnoreCase(
                        tenantId, search.trim(), pageable);
            } else if (activeOnly) {
                page = examinationTypeRepository.findBySchoolIdAndActiveTrueAndDeletedAtIsNull(tenantId, pageable);
            } else {
                page = examinationTypeRepository.findBySchoolIdAndDeletedAtIsNull(tenantId, pageable);
            }
        } else {
            page = examinationTypeRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public ExaminationTypeDto getById(Long id) {
        return mapToDto(findAccessible(id));
    }

    @Transactional
    public ExaminationTypeDto create(CreateExaminationTypeRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (examinationTypeRepository.existsBySchoolIdAndNameIgnoreCase(tenantId, request.getName())) {
            throw new ConflictException("Examination type name already exists for this school");
        }

        ExaminationType examinationType = new ExaminationType();
        examinationType.setSchoolId(tenantId);
        examinationType.setName(request.getName());
        examinationType.setCode(request.getCode());
        examinationType.setDescription(request.getDescription());
        examinationType.setActive(request.getActive() != null ? request.getActive() : true);

        examinationTypeRepository.save(examinationType);

        auditLogService.log("EXAMINATION_TYPE_CREATE", "ExaminationType", examinationType.getId(),
                "Created examination type " + examinationType.getName(), null, null, null);

        return mapToDto(examinationType);
    }

    @Transactional
    public ExaminationTypeDto update(Long id, CreateExaminationTypeRequest request) {
        var examinationType = findAccessible(id);

        Long tenantId = TenantContext.getTenantId();

        if (!examinationType.getName().equalsIgnoreCase(request.getName()) &&
                examinationTypeRepository.existsBySchoolIdAndNameIgnoreCaseAndIdNot(
                        examinationType.getSchoolId(), request.getName(), id)) {
            throw new ConflictException("Examination type name already exists for this school");
        }

        examinationType.setName(request.getName());
        examinationType.setCode(request.getCode());
        examinationType.setDescription(request.getDescription());
        if (request.getActive() != null) {
            examinationType.setActive(request.getActive());
        }

        examinationTypeRepository.save(examinationType);

        auditLogService.log("EXAMINATION_TYPE_UPDATE", "ExaminationType", examinationType.getId(),
                "Updated examination type " + examinationType.getName(), null, null, null);

        return mapToDto(examinationType);
    }

    @Transactional
    public ExaminationTypeDto setActive(Long id, Boolean active) {
        var examinationType = findAccessible(id);

        examinationType.setActive(active);
        examinationTypeRepository.save(examinationType);

        auditLogService.log("EXAMINATION_TYPE_STATUS", "ExaminationType", examinationType.getId(),
                "Set examination type " + examinationType.getName() + " active=" + active, null, null, null);

        return mapToDto(examinationType);
    }

    @Transactional
    public void delete(Long id) {
        var examinationType = findAccessible(id);

        Long tenantId = TenantContext.getTenantId();

        boolean inUse = examinationRepository.existsBySchoolIdAndTypeId(tenantId, id);
        if (inUse) {
            throw new ConflictException("Cannot delete examination type because it is referenced by existing examinations");
        }

        examinationType.setDeletedAt(LocalDateTime.now());
        examinationTypeRepository.save(examinationType);

        auditLogService.log("EXAMINATION_TYPE_DELETE", "ExaminationType", examinationType.getId(),
                "Deleted examination type " + examinationType.getName(), null, null, null);
    }

    private ExaminationType findAccessible(Long id) {
        var examinationType = examinationTypeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination type not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(examinationType.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
        return examinationType;
    }

    private ExaminationTypeDto mapToDto(ExaminationType entity) {
        ExaminationTypeDto dto = new ExaminationTypeDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
