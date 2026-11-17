package com.edumanage.branding.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.branding.dto.SchoolBrandingDto;
import com.edumanage.branding.entity.SchoolBranding;
import com.edumanage.branding.repository.SchoolBrandingRepository;
import com.edumanage.branding.storage.FileStorageService;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class SchoolBrandingService {

    private final SchoolBrandingRepository brandingRepository;
    private final SchoolRepository schoolRepository;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;

    public SchoolBrandingService(SchoolBrandingRepository brandingRepository, SchoolRepository schoolRepository,
                                 AuditLogService auditLogService, FileStorageService fileStorageService) {
        this.brandingRepository = brandingRepository;
        this.schoolRepository = schoolRepository;
        this.auditLogService = auditLogService;
        this.fileStorageService = fileStorageService;
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

    /* ------------------------------------------------------------------ */
    /* Super Admin (platform) operations - no tenant-context reliance     */
    /* ------------------------------------------------------------------ */

    public SchoolBrandingDto getBrandingForSchool(Long schoolId) {
        requireSchool(schoolId);
        var branding = brandingRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultBranding(schoolId));
        return mapToDto(branding);
    }

    @Transactional
    public SchoolBrandingDto updateBrandingForSchool(Long schoolId, SchoolBrandingDto request) {
        requireSchool(schoolId);

        var branding = brandingRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultBranding(schoolId));

        String previousLogoUrl = branding.getLogoUrl();

        branding.setPrimaryColor(request.getPrimaryColor());
        branding.setSecondaryColor(request.getSecondaryColor());
        branding.setAccentColor(request.getAccentColor());
        branding.setBackgroundColor(request.getBackgroundColor());
        branding.setTextColor(request.getTextColor());
        branding.setLogoUrl(request.getLogoUrl());
        branding.setFaviconUrl(request.getFaviconUrl());

        brandingRepository.save(branding);

        if (previousLogoUrl != null && !previousLogoUrl.equals(branding.getLogoUrl())) {
            fileStorageService.deleteLogo(extractFileName(previousLogoUrl));
        }

        auditLogService.logForSchool("BRANDING_UPDATE", "SchoolBranding", branding.getId(),
                "Super Admin updated branding for school " + schoolId, null, null, null, schoolId);

        return mapToDto(branding);
    }

    @Transactional
    public SchoolBrandingDto uploadLogo(Long schoolId, String logoUrl) {
        requireSchool(schoolId);

        var branding = brandingRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultBranding(schoolId));

        String previousLogoUrl = branding.getLogoUrl();
        branding.setLogoUrl(logoUrl);
        brandingRepository.save(branding);

        if (previousLogoUrl != null) {
            fileStorageService.deleteLogo(extractFileName(previousLogoUrl));
        }

        auditLogService.logForSchool("LOGO_UPLOAD", "SchoolBranding", branding.getId(),
                "Super Admin uploaded logo for school " + schoolId, null, null, null, schoolId);

        return mapToDto(branding);
    }

    @Transactional
    public SchoolBrandingDto removeLogo(Long schoolId) {
        requireSchool(schoolId);

        var branding = brandingRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultBranding(schoolId));

        String previousLogoUrl = branding.getLogoUrl();
        branding.setLogoUrl(null);
        brandingRepository.save(branding);

        if (previousLogoUrl != null) {
            fileStorageService.deleteLogo(extractFileName(previousLogoUrl));
        }

        auditLogService.logForSchool("LOGO_REMOVE", "SchoolBranding", branding.getId(),
                "Super Admin removed logo for school " + schoolId, null, null, null, schoolId);

        return mapToDto(branding);
    }

    @Transactional
    public SchoolBrandingDto resetBranding(Long schoolId) {
        requireSchool(schoolId);

        var branding = brandingRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultBranding(schoolId));

        String previousLogoUrl = branding.getLogoUrl();

        branding.setPrimaryColor("#3b82f6");
        branding.setSecondaryColor("#64748b");
        branding.setAccentColor("#f59e0b");
        branding.setBackgroundColor("#ffffff");
        branding.setTextColor("#0f172a");
        branding.setLogoUrl(null);
        branding.setFaviconUrl(null);

        brandingRepository.save(branding);

        if (previousLogoUrl != null) {
            fileStorageService.deleteLogo(extractFileName(previousLogoUrl));
        }

        auditLogService.logForSchool("BRANDING_RESET", "SchoolBranding", branding.getId(),
                "Super Admin reset branding to defaults for school " + schoolId, null, null, null, schoolId);

        return mapToDto(branding);
    }

    private void requireSchool(Long schoolId) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found");
        }
    }

    private String extractFileName(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return null;
        }
        try {
            String path = new URI(logoUrl).getPath();
            if (path == null || path.isEmpty()) {
                return null;
            }
            String[] segments = path.split("/");
            String fileName = segments[segments.length - 1];
            return fileName.isEmpty() ? null : fileName;
        } catch (URISyntaxException ex) {
            int slash = logoUrl.lastIndexOf('/');
            return slash >= 0 ? logoUrl.substring(slash + 1) : null;
        }
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
