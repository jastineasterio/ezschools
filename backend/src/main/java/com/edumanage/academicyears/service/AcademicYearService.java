package com.edumanage.academicyears.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.academicyears.dto.AcademicYearDto;
import com.edumanage.academicyears.dto.CreateAcademicYearRequest;
import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final AuditLogService auditLogService;

    public AcademicYearService(AcademicYearRepository academicYearRepository, AuditLogService auditLogService) {
        this.academicYearRepository = academicYearRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<AcademicYearDto> listBySchool(Pageable pageable, String search) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null && !TenantContext.isSuperAdmin()) {
            throw new ForbiddenException("A school assignment is required");
        }
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<AcademicYear> page = academicYearRepository.search(tenantId, normalizedSearch, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public AcademicYearDto getById(Long id) {
        var academicYear = academicYearRepository.findById(id)
                .filter(ay -> !ay.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        TenantContext.assertTenantAccess(academicYear.getSchoolId());

        return mapToDto(academicYear);
    }

    @Transactional
    public AcademicYearDto create(CreateAcademicYearRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        validateDateRange(request);

        if (academicYearRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new ConflictException("Academic year name already exists for this school");
        }
        validateNoOverlap(schoolId, request.getStartDate(), request.getEndDate(), null);
        validateCurrentYearChange(schoolId, request.getIsCurrent(), null);

        AcademicYear academicYear = new AcademicYear();
        academicYear.setSchoolId(schoolId);
        academicYear.setName(request.getName());
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());
        academicYear.setStatus(request.getStatus() != null ? request.getStatus() : "UPCOMING");
        academicYear.setCurrent(request.getIsCurrent() != null && request.getIsCurrent());

        academicYearRepository.save(academicYear);

        auditLogService.log("ACADEMIC_YEAR_CREATE", "AcademicYear", academicYear.getId(),
                "Created academic year " + academicYear.getName(), null, null, null);

        return mapToDto(academicYear);
    }

    @Transactional
    public AcademicYearDto update(Long id, CreateAcademicYearRequest request) {
        var academicYear = academicYearRepository.findById(id)
                .filter(ay -> !ay.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        TenantContext.assertTenantAccess(academicYear.getSchoolId());
        validateDateRange(request);

        Long schoolId = academicYear.getSchoolId();

        if (!academicYear.getName().equals(request.getName()) &&
                academicYearRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new ConflictException("Academic year name already exists for this school");
        }
        validateNoOverlap(schoolId, request.getStartDate(), request.getEndDate(), id);
        validateCurrentYearChange(schoolId, request.getIsCurrent(), id);

        academicYear.setName(request.getName());
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            academicYear.setStatus(request.getStatus());
        }
        if (request.getIsCurrent() != null) {
            academicYear.setCurrent(request.getIsCurrent());
        }

        academicYearRepository.save(academicYear);

        auditLogService.log("ACADEMIC_YEAR_UPDATE", "AcademicYear", academicYear.getId(),
                "Updated academic year " + academicYear.getName(), null, null, null);

        return mapToDto(academicYear);
    }

    @Transactional
    public AcademicYearDto setCurrent(Long id) {
        var academicYear = academicYearRepository.findById(id)
                .filter(ay -> !ay.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        TenantContext.assertTenantAccess(academicYear.getSchoolId());

        academicYearRepository.findBySchoolIdAndIsCurrentTrueAndDeletedAtIsNull(academicYear.getSchoolId())
                .ifPresent(current -> {
                    current.setCurrent(false);
                    academicYearRepository.save(current);
                });

        academicYear.setCurrent(true);
        academicYearRepository.save(academicYear);

        auditLogService.log("ACADEMIC_YEAR_SET_CURRENT", "AcademicYear", academicYear.getId(),
                "Set academic year " + academicYear.getName() + " as current", null, null, null);

        return mapToDto(academicYear);
    }

    private void validateDateRange(CreateAcademicYearRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date must be on or before the end date");
        }
    }

    private void validateNoOverlap(Long schoolId, java.time.LocalDate startDate,
                                   java.time.LocalDate endDate, Long excludeId) {
        if (academicYearRepository.existsOverlapping(schoolId, startDate, endDate, excludeId)) {
            throw new ConflictException("Academic year dates overlap another academic year for this school");
        }
    }

    private void validateCurrentYearChange(Long schoolId, Boolean requestedCurrent, Long excludeId) {
        if (!Boolean.TRUE.equals(requestedCurrent)) {
            return;
        }
        academicYearRepository.findBySchoolIdAndIsCurrentTrueAndDeletedAtIsNull(schoolId)
                .filter(current -> !current.getId().equals(excludeId))
                .ifPresent(current -> {
                    throw new ConflictException(
                            "A current academic year already exists. Review it before setting another year as current");
                });
    }

    private AcademicYearDto mapToDto(AcademicYear entity) {
        AcademicYearDto dto = new AcademicYearDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());
        dto.setCurrent(entity.isCurrent());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
