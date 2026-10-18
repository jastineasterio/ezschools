package com.edumanage.examinations.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.examinations.dto.CreateExaminationRequest;
import com.edumanage.examinations.dto.ExaminationDto;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.examinationtypes.repository.ExaminationTypeRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ExaminationTypeRepository examinationTypeRepository;
    private final AuditLogService auditLogService;

    public ExaminationService(ExaminationRepository examinationRepository,
                              AcademicYearRepository academicYearRepository,
                              ExaminationTypeRepository examinationTypeRepository,
                              AuditLogService auditLogService) {
        this.examinationRepository = examinationRepository;
        this.academicYearRepository = academicYearRepository;
        this.examinationTypeRepository = examinationTypeRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<ExaminationDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Examination> page;
        if (tenantId != null) {
            page = examinationRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = examinationRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(examination -> mapToDto(examination, null, null)));
    }

    public ExaminationDto getById(Long id) {
        var examination = examinationRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(examination.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(examination, null, null);
    }

    @Transactional
    public ExaminationDto create(CreateExaminationRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        if (!tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Academic year does not belong to this school");
        }

        var type = examinationTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Examination type not found"));

        if (!tenantId.equals(type.getSchoolId())) {
            throw new ForbiddenException("Examination type does not belong to this school");
        }

        Examination examination = new Examination();
        examination.setSchoolId(tenantId);
        examination.setAcademicYearId(request.getAcademicYearId());
        examination.setName(request.getName());
        examination.setTypeId(request.getTypeId());
        examination.setStartDate(request.getStartDate());
        examination.setEndDate(request.getEndDate());
        examination.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        examination.setDescription(request.getDescription());

        examinationRepository.save(examination);

        auditLogService.log("EXAMINATION_CREATE", "Examination", examination.getId(),
                "Created examination " + examination.getName(), null, null, null);

        return mapToDto(examination, academicYear.getName(), type.getName());
    }

    @Transactional
    public ExaminationDto update(Long id, CreateExaminationRequest request) {
        var examination = examinationRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(examination.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        if (!tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Academic year does not belong to this school");
        }

        var type = examinationTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Examination type not found"));

        if (!tenantId.equals(type.getSchoolId())) {
            throw new ForbiddenException("Examination type does not belong to this school");
        }

        examination.setAcademicYearId(request.getAcademicYearId());
        examination.setName(request.getName());
        examination.setTypeId(request.getTypeId());
        examination.setStartDate(request.getStartDate());
        examination.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            examination.setStatus(request.getStatus());
        }
        examination.setDescription(request.getDescription());

        examinationRepository.save(examination);

        auditLogService.log("EXAMINATION_UPDATE", "Examination", examination.getId(),
                "Updated examination " + examination.getName(), null, null, null);

        return mapToDto(examination, academicYear.getName(), type.getName());
    }

    @Transactional
    public ExaminationDto updateStatus(Long id, String status) {
        var examination = examinationRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(examination.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        examination.setStatus(status);
        examinationRepository.save(examination);

        auditLogService.log("EXAMINATION_UPDATE_STATUS", "Examination", examination.getId(),
                "Updated examination " + examination.getName() + " status to " + status, null, null, null);

        return mapToDto(examination, null, null);
    }

    private ExaminationDto mapToDto(Examination entity, String academicYearName, String typeName) {
        ExaminationDto dto = new ExaminationDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setAcademicYearId(entity.getAcademicYearId());
        dto.setAcademicYearName(academicYearName);
        dto.setName(entity.getName());
        dto.setTypeId(entity.getTypeId());
        dto.setTypeName(typeName);
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
