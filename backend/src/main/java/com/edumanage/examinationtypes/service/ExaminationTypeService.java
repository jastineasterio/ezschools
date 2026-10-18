package com.edumanage.examinationtypes.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.examinationtypes.dto.CreateExaminationTypeRequest;
import com.edumanage.examinationtypes.dto.ExaminationTypeDto;
import com.edumanage.examinationtypes.entity.ExaminationType;
import com.edumanage.examinationtypes.repository.ExaminationTypeRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExaminationTypeService {

    private final ExaminationTypeRepository examinationTypeRepository;
    private final AuditLogService auditLogService;

    public ExaminationTypeService(ExaminationTypeRepository examinationTypeRepository, AuditLogService auditLogService) {
        this.examinationTypeRepository = examinationTypeRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<ExaminationTypeDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<ExaminationType> page;
        if (tenantId != null) {
            page = examinationTypeRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = examinationTypeRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public ExaminationTypeDto getById(Long id) {
        var examinationType = examinationTypeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination type not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(examinationType.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(examinationType);
    }

    @Transactional
    public ExaminationTypeDto create(CreateExaminationTypeRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (examinationTypeRepository.existsBySchoolIdAndName(tenantId, request.getName())) {
            throw new ConflictException("Examination type name already exists for this school");
        }

        ExaminationType examinationType = new ExaminationType();
        examinationType.setSchoolId(tenantId);
        examinationType.setName(request.getName());
        examinationType.setCode(request.getCode());
        examinationType.setDescription(request.getDescription());

        examinationTypeRepository.save(examinationType);

        auditLogService.log("EXAMINATION_TYPE_CREATE", "ExaminationType", examinationType.getId(),
                "Created examination type " + examinationType.getName(), null, null, null);

        return mapToDto(examinationType);
    }

    @Transactional
    public ExaminationTypeDto update(Long id, CreateExaminationTypeRequest request) {
        var examinationType = examinationTypeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination type not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(examinationType.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!examinationType.getName().equals(request.getName()) &&
                examinationTypeRepository.existsBySchoolIdAndName(examinationType.getSchoolId(), request.getName())) {
            throw new ConflictException("Examination type name already exists for this school");
        }

        examinationType.setName(request.getName());
        examinationType.setCode(request.getCode());
        examinationType.setDescription(request.getDescription());

        examinationTypeRepository.save(examinationType);

        auditLogService.log("EXAMINATION_TYPE_UPDATE", "ExaminationType", examinationType.getId(),
                "Updated examination type " + examinationType.getName(), null, null, null);

        return mapToDto(examinationType);
    }

    private ExaminationTypeDto mapToDto(ExaminationType entity) {
        ExaminationTypeDto dto = new ExaminationTypeDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
