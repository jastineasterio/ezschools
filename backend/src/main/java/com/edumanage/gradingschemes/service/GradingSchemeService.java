package com.edumanage.gradingschemes.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
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
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradingSchemeService {

    private final GradingSchemeRepository gradingSchemeRepository;
    private final GradingSchemeRangeRepository gradingSchemeRangeRepository;
    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final AuditLogService auditLogService;

    public GradingSchemeService(GradingSchemeRepository gradingSchemeRepository,
                                GradingSchemeRangeRepository gradingSchemeRangeRepository,
                                ExaminationSubjectRepository examinationSubjectRepository,
                                AuditLogService auditLogService) {
        this.gradingSchemeRepository = gradingSchemeRepository;
        this.gradingSchemeRangeRepository = gradingSchemeRangeRepository;
        this.examinationSubjectRepository = examinationSubjectRepository;
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
        if (schemeId == null) {
            throw new BadRequestException("Scheme ID is required");
        }

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
        validateRanges(schemeId, null);

        auditLogService.log("GRADING_SCHEME_RANGE_ADD", "GradingSchemeRange", range.getId(),
                "Added grading scheme range " + range.getGrade() + " to scheme " + scheme.getName(), null, null, null);

        return mapRangeToDto(range);
    }

    @Transactional
    public GradingSchemeRangeDto updateRange(Long schemeId, Long rangeId, CreateGradingSchemeRangeRequest request) {
        var scheme = gradingSchemeRepository.findById(schemeId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var range = gradingSchemeRangeRepository.findById(rangeId)
                .filter(r -> !r.isDeleted() && schemeId.equals(r.getSchemeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme range not found"));

        range.setGrade(request.getGrade());
        range.setMinMark(new java.math.BigDecimal(request.getMinMark()));
        range.setMaxMark(new java.math.BigDecimal(request.getMaxMark()));
        range.setGradePoint(request.getGradePoint() != null ? new java.math.BigDecimal(request.getGradePoint()) : null);
        range.setRemark(request.getRemark());

        validateRanges(schemeId, rangeId);

        gradingSchemeRangeRepository.save(range);

        auditLogService.log("GRADING_SCHEME_RANGE_UPDATE", "GradingSchemeRange", range.getId(),
                "Updated grading scheme range " + range.getGrade() + " in scheme " + scheme.getName(), null, null, null);

        return mapRangeToDto(range);
    }

    @Transactional
    public void deleteRange(Long schemeId, Long rangeId) {
        var scheme = gradingSchemeRepository.findById(schemeId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var range = gradingSchemeRangeRepository.findById(rangeId)
                .filter(r -> !r.isDeleted() && schemeId.equals(r.getSchemeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme range not found"));

        gradingSchemeRangeRepository.delete(range);

        validateRanges(schemeId, null);

        auditLogService.log("GRADING_SCHEME_RANGE_DELETE", "GradingSchemeRange", rangeId,
                "Deleted grading scheme range " + range.getGrade() + " from scheme " + scheme.getName(), null, null, null);
    }

    @Transactional
    public GradingSchemeDto toggleStatus(Long schemeId, String status) {
        var scheme = gradingSchemeRepository.findById(schemeId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        String normalized = (status == null ? "" : status.trim().toUpperCase());
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new BadRequestException("Status must be ACTIVE or INACTIVE");
        }
        scheme.setStatus(normalized);
        gradingSchemeRepository.save(scheme);

        auditLogService.log("GRADING_SCHEME_STATUS", "GradingScheme", scheme.getId(),
                "Changed grading scheme status to " + normalized, null, null, null);

        List<GradingSchemeRangeDto> ranges = gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId).stream()
                .map(this::mapRangeToDto)
                .collect(Collectors.toList());
        return mapToDto(scheme, ranges);
    }

    @Transactional
    public void deleteScheme(Long schemeId) {
        var scheme = gradingSchemeRepository.findById(schemeId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        boolean inUse = examinationSubjectRepository.existsByGradingSchemeId(schemeId);
        if (inUse) {
            throw new ConflictException("Grading scheme is in use by one or more examination subjects and cannot be deleted");
        }

        gradingSchemeRangeRepository.deleteBySchemeId(schemeId);
        gradingSchemeRepository.delete(scheme);

        auditLogService.log("GRADING_SCHEME_DELETE", "GradingScheme", schemeId,
                "Deleted grading scheme " + scheme.getName(), null, null, null);
    }

    public void validateRangesAgainstMaximumMarks(Long schemeId, BigDecimal maximumMarks) {
        if (maximumMarks == null || maximumMarks.signum() <= 0) {
            throw new BadRequestException("Examination maximum marks must be greater than zero");
        }

        Long tenantId = TenantContext.getTenantId();
        var scheme = gradingSchemeRepository.findById(schemeId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Grading scheme does not belong to this school");
        }

        List<GradingSchemeRange> ranges = gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId)
                .stream()
                .sorted(Comparator.comparing(GradingSchemeRange::getMinMark))
                .toList();

        if (ranges.isEmpty()) {
            throw new BadRequestException("Grading scheme has no grade ranges configured");
        }

        BigDecimal previousMax = null;
        for (GradingSchemeRange range : ranges) {
            BigDecimal min = range.getMinMark();
            BigDecimal max = range.getMaxMark();
            String grade = range.getGrade();
            if (min == null || max == null) {
                throw new BadRequestException("Grade '" + grade + "' must have both minimum and maximum marks");
            }
            if (min.signum() < 0 || max.signum() < 0) {
                throw new BadRequestException("Grade '" + grade + "' marks must be non-negative");
            }
            if (min.compareTo(max) > 0) {
                throw new BadRequestException("Grade '" + grade + "' minimum mark must not exceed maximum mark");
            }
            if (max.compareTo(maximumMarks) > 0) {
                throw new BadRequestException("Grade '" + grade + "' maximum mark " + max
                        + " exceeds the examination maximum marks " + maximumMarks);
            }
            if (previousMax == null) {
                if (min.compareTo(BigDecimal.ZERO) != 0) {
                    throw new BadRequestException("Grading scale must start at 0 (grade '" + grade + "' starts at " + min + ")");
                }
            } else {
                if (min.compareTo(previousMax) <= 0) {
                    throw new BadRequestException("Overlapping grading ranges detected around grade '" + grade + "'");
                }
                if (min.compareTo(previousMax.add(BigDecimal.ONE)) != 0) {
                    throw new BadRequestException("Gap in grading scale between " + previousMax + " and " + min
                            + ". Every score from 0 to " + maximumMarks + " must map to exactly one grade.");
                }
            }
            previousMax = max;
        }

        if (previousMax.compareTo(maximumMarks) != 0) {
            throw new BadRequestException("Grading scheme ranges must cover 0 through " + maximumMarks
                    + " without gaps (configured highest maximum is " + previousMax + ")");
        }
    }

    private void validateRanges(Long schemeId, Long excludeRangeId) {
        List<GradingSchemeRange> ranges = gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId)
                .stream()
                .filter(r -> excludeRangeId == null || !r.getId().equals(excludeRangeId))
                .sorted(Comparator.comparing(GradingSchemeRange::getMinMark))
                .toList();

        if (ranges.isEmpty()) {
            return;
        }

        BigDecimal previousMax = null;
        boolean first = true;
        for (GradingSchemeRange range : ranges) {
            BigDecimal min = range.getMinMark();
            BigDecimal max = range.getMaxMark();
            String grade = range.getGrade();
            if (min == null || max == null) {
                throw new BadRequestException("Grade '" + grade + "' must have both minimum and maximum marks");
            }
            if (min.signum() < 0 || max.signum() < 0) {
                throw new BadRequestException("Grade '" + grade + "' marks must be non-negative");
            }
            if (min.compareTo(max) > 0) {
                throw new BadRequestException("Grade '" + grade + "' minimum mark must not exceed maximum mark");
            }
            if (first && min.compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("Grading scale must start at 0 (grade '" + grade + "' starts at " + min + ")");
            }
            first = false;
            if (previousMax != null) {
                if (min.compareTo(previousMax) <= 0) {
                    throw new BadRequestException("Overlapping grading ranges detected around grade '" + grade + "'");
                }
                if (min.compareTo(previousMax.add(BigDecimal.ONE)) > 0) {
                    throw new BadRequestException("Gap in grading scale between " + previousMax + " and " + min
                            + ". Every score must map to exactly one grade.");
                }
            }
            previousMax = max;
        }
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
