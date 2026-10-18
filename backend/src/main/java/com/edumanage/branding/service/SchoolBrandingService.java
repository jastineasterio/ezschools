package com.edumanage.branding.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.branding.dto.SchoolBrandingDto;
import com.edumanage.branding.entity.SchoolBranding;
import com.edumanage.branding.repository.SchoolBrandingRepository;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolBrandingService {

    private final SchoolBrandingRepository brandingRepository;
    private final SchoolRepository schoolRepository;
    private final AuditLogService auditLogService;

    public SchoolBrandingService(SchoolBrandingRepository brandingRepository, SchoolRepository schoolRepository,
                                 AuditLogService auditLogService) {
        this.brandingRepository = brandingRepository;
        this.schoolRepository = schoolRepository;
        this.auditLogService = auditLogService;
    }

    public SchoolBrandingDto getBranding(Long schoolId) {
        validateSchoolAccess(schoolId);
        var branding = brandingRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultBranding(schoolId));
        return mapToDto(branding);
    }

    @Transactional
    public SchoolBrandingDto updateBranding(Long schoolId, SchoolBrandingDto request) {
        validateSchoolAccess(schoolId);

        var branding = brandingRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultBranding(schoolId));

        branding.setPrimaryColor(request.getPrimaryColor());
        branding.setSecondaryColor(request.getSecondaryColor());
        branding.setAccentColor(request.getAccentColor());
        branding.setBackgroundColor(request.getBackgroundColor());
        branding.setTextColor(request.getTextColor());
        branding.setLogoUrl(request.getLogoUrl());
        branding.setFaviconUrl(request.getFaviconUrl());

        brandingRepository.save(branding);

        auditLogService.log("BRANDING_UPDATE", "SchoolBranding", branding.getId(),
                "Updated branding for school " + schoolId, null, null, null);

        return mapToDto(branding);
    }

    private void validateSchoolAccess(Long schoolId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(schoolId)) {
            throw new ForbiddenException("Access denied");
        }
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found");
        }
    }

    private SchoolBranding createDefaultBranding(Long schoolId) {
        SchoolBranding branding = new SchoolBranding();
        branding.setSchoolId(schoolId);
        branding.setPrimaryColor("#3b82f6");
        branding.setSecondaryColor("#64748b");
        branding.setAccentColor("#f59e0b");
        branding.setBackgroundColor("#ffffff");
        branding.setTextColor("#0f172a");
        return brandingRepository.save(branding);
    }

    private SchoolBrandingDto mapToDto(SchoolBranding branding) {
        SchoolBrandingDto dto = new SchoolBrandingDto();
        dto.setId(branding.getId());
        dto.setSchoolId(branding.getSchoolId());
        dto.setPrimaryColor(branding.getPrimaryColor());
        dto.setSecondaryColor(branding.getSecondaryColor());
        dto.setAccentColor(branding.getAccentColor());
        dto.setBackgroundColor(branding.getBackgroundColor());
        dto.setTextColor(branding.getTextColor());
        dto.setLogoUrl(branding.getLogoUrl());
        dto.setFaviconUrl(branding.getFaviconUrl());
        dto.setCreatedAt(branding.getCreatedAt());
        dto.setUpdatedAt(branding.getUpdatedAt());
        return dto;
    }
}
