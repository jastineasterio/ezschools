package com.edumanage.gradingschemes.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.gradingschemes.dto.CreateGradingSchemeRangeRequest;
import com.edumanage.gradingschemes.dto.CreateGradingSchemeRequest;
import com.edumanage.gradingschemes.dto.GradingSchemeDto;
import com.edumanage.gradingschemes.dto.GradingSchemeRangeDto;
import com.edumanage.gradingschemes.entity.GradingScheme;
import com.edumanage.gradingschemes.repository.GradingSchemeRepository;
import com.edumanage.gradingschemeranges.entity.GradingSchemeRange;
import com.edumanage.gradingschemeranges.repository.GradingSchemeRangeRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradingSchemeService {

    private final GradingSchemeRepository gradingSchemeRepository;
    private final GradingSchemeRangeRepository gradingSchemeRangeRepository;
    private final AuditLogService auditLogService;

    public GradingSchemeService(GradingSchemeRepository gradingSchemeRepository,
                               GradingSchemeRangeRepository gradingSchemeRangeRepository,
                               AuditLogService auditLogService) {
        this.gradingSchemeRepository = gradingSchemeRepository;
        this.gradingSchemeRangeRepository = gradingSchemeRangeRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<GradingSchemeDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<GradingScheme> page;
        if (tenantId != null) {
            page = gradingSchemeRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = gradingSchemeRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(scheme -> mapToDto(scheme, gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(scheme.getId()).stream()
                .map(this::mapRangeToDto)
                .collect(Collectors.toList()))));
    }

    public GradingSchemeDto getById(Long id) {
        var scheme = gradingSchemeRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        List<GradingSchemeRangeDto> ranges = gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(id).stream()
                .map(this::mapRangeToDto)
                .collect(Collectors.toList());

        return mapToDto(scheme, ranges);
    }

    @Transactional
    public GradingSchemeDto create(CreateGradingSchemeRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        GradingScheme scheme = new GradingScheme();
        scheme.setSchoolId(tenantId);
        scheme.setName(request.getName());
        scheme.setDescription(request.getDescription());
        scheme.setDefault(true && request.isDefault());
        scheme.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        if (Boolean.TRUE.equals(request.isDefault())) {
            gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(tenantId)
                    .ifPresent(current -> {
                        current.setDefault(false);
                        gradingSchemeRepository.save(current);
                    });
        }

        gradingSchemeRepository.save(scheme);

        auditLogService.log("GRADING_SCHEME_CREATE", "GradingScheme", scheme.getId(),
                "Created grading scheme " + scheme.getName(), null, null, null);

        return mapToDto(scheme, new ArrayList<>());
    }

    @Transactional
    public GradingSchemeDto update(Long id, CreateGradingSchemeRequest request) {
        var scheme = gradingSchemeRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        scheme.setName(request.getName());
        scheme.setDescription(request.getDescription());
        if (true) {
            scheme.setDefault(request.isDefault());
        }
        if (request.getStatus() != null) {
            scheme.setStatus(request.getStatus());
        }

        if (Boolean.TRUE.equals(request.isDefault())) {
            gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(scheme.getSchoolId())
                    .ifPresent(current -> {
                        if (!current.getId().equals(scheme.getId())) {
                            current.setDefault(false);
                            gradingSchemeRepository.save(current);
                        }
                    });
        }

        gradingSchemeRepository.save(scheme);

        auditLogService.log("GRADING_SCHEME_UPDATE", "GradingScheme", scheme.getId(),
                "Updated grading scheme " + scheme.getName(), null, null, null);

        List<GradingSchemeRangeDto> ranges = gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(id).stream()
                .map(this::mapRangeToDto)
                .collect(Collectors.toList());

        return mapToDto(scheme, ranges);
    }

    @Transactional
    public GradingSchemeRangeDto addRange(Long schemeId, CreateGradingSchemeRangeRequest request) {
        var scheme = gradingSchemeRepository.findById(schemeId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        GradingSchemeRange range = new GradingSchemeRange();
        range.setSchemeId(schemeId);
        range.setGrade(request.getGrade());
        range.setMinMark(new java.math.BigDecimal(request.getMinMark()));
        range.setMaxMark(new java.math.BigDecimal(request.getMaxMark()));
        range.setGradePoint(request.getGradePoint() != null ? new java.math.BigDecimal(request.getGradePoint()) : null);
        range.setRemark(request.getRemark());

        gradingSchemeRangeRepository.save(range);

        auditLogService.log("GRADING_SCHEME_RANGE_ADD", "GradingSchemeRange", range.getId(),
                "Added grading scheme range " + range.getGrade() + " to scheme " + scheme.getName(), null, null, null);

        return mapRangeToDto(range);
    }

    private GradingSchemeDto mapToDto(GradingScheme entity, List<GradingSchemeRangeDto> ranges) {
        GradingSchemeDto dto = new GradingSchemeDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setDefault(entity.isDefault());
        dto.setStatus(entity.getStatus());
        dto.setRanges(ranges);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private GradingSchemeRangeDto mapRangeToDto(GradingSchemeRange entity) {
        GradingSchemeRangeDto dto = new GradingSchemeRangeDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchemeId(entity.getSchemeId());
        dto.setGrade(entity.getGrade());
        dto.setMinMark(entity.getMinMark() != null ? entity.getMinMark().intValue() : null);
        dto.setMaxMark(entity.getMaxMark() != null ? entity.getMaxMark().intValue() : null);
        dto.setGradePoint(entity.getGradePoint() != null ? entity.getGradePoint().doubleValue() : null);
        dto.setRemark(entity.getRemark());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
