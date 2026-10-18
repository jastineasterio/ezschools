package com.edumanage.gradingschemeranges.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.gradingschemes.entity.GradingScheme;
import com.edumanage.gradingschemes.repository.GradingSchemeRepository;
import com.edumanage.gradingschemeranges.entity.GradingSchemeRange;
import com.edumanage.gradingschemeranges.repository.GradingSchemeRangeRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradingSchemeRangeService {

    private final GradingSchemeRangeRepository gradingSchemeRangeRepository;
    private final GradingSchemeRepository gradingSchemeRepository;

    public GradingSchemeRangeService(GradingSchemeRangeRepository gradingSchemeRangeRepository,
                                     GradingSchemeRepository gradingSchemeRepository) {
        this.gradingSchemeRangeRepository = gradingSchemeRangeRepository;
        this.gradingSchemeRepository = gradingSchemeRepository;
    }

    public List<com.edumanage.gradingschemes.dto.GradingSchemeRangeDto> listByScheme(Long schemeId) {
        var scheme = gradingSchemeRepository.findById(schemeId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private com.edumanage.gradingschemes.dto.GradingSchemeRangeDto mapToDto(GradingSchemeRange entity) {
        com.edumanage.gradingschemes.dto.GradingSchemeRangeDto dto = new com.edumanage.gradingschemes.dto.GradingSchemeRangeDto();
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
