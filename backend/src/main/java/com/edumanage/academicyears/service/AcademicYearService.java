package com.edumanage.academicyears.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final AuditLogService auditLogService;

    public AcademicYearService(AcademicYearRepository academicYearRepository, AuditLogService auditLogService) {
        this.academicYearRepository = academicYearRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<AcademicYearDto> listBySchool(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<AcademicYear> page;
        if (tenantId != null) {
            List<AcademicYear> all = academicYearRepository.findBySchoolIdOrderByStartDateDesc(tenantId);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), all.size());
            List<AcademicYear> paged = start > all.size() ? List.of() : all.subList(start, end);
            page = new org.springframework.data.domain.PageImpl<>(paged, pageable, all.size());
        } else {
            page = academicYearRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public AcademicYearDto getById(Long id) {
        var academicYear = academicYearRepository.findById(id)
                .filter(ay -> !ay.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(academicYear);
    }

    @Transactional
    public AcademicYearDto create(CreateAcademicYearRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long schoolId = tenantId != null ? tenantId : null;
        if (schoolId == null) {
            throw new BadRequestException("School ID is required");
        }

        if (academicYearRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new ConflictException("Academic year name already exists for this school");
        }

        AcademicYear academicYear = new AcademicYear();
        academicYear.setSchoolId(schoolId);
        academicYear.setName(request.getName());
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());
        academicYear.setStatus(request.getStatus() != null ? request.getStatus() : "UPCOMING");
        academicYear.setCurrent(request.getIsCurrent() != null && request.getIsCurrent());

        if (Boolean.TRUE.equals(request.getIsCurrent())) {
            academicYearRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                    .ifPresent(current -> {
                        current.setCurrent(false);
                        academicYearRepository.save(current);
                    });
        }

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

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Long schoolId = academicYear.getSchoolId();

        if (!academicYear.getName().equals(request.getName()) &&
                academicYearRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new ConflictException("Academic year name already exists for this school");
        }

        academicYear.setName(request.getName());
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            academicYear.setStatus(request.getStatus());
        }
        if (request.getIsCurrent() != null) {
            academicYear.setCurrent(request.getIsCurrent());
        }

        if (Boolean.TRUE.equals(request.getIsCurrent())) {
            academicYearRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                    .ifPresent(current -> {
                        if (!current.getId().equals(academicYear.getId())) {
                            current.setCurrent(false);
                            academicYearRepository.save(current);
                        }
                    });
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

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        academicYearRepository.findBySchoolIdAndIsCurrentTrue(academicYear.getSchoolId())
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
