package com.edumanage.branding.service;

import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.branding.dto.SchoolBrandingDto;
import com.edumanage.branding.entity.SchoolBranding;
import com.edumanage.branding.repository.SchoolBrandingRepository;
import com.edumanage.branding.storage.FileStorageService;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolBrandingServicePlatformTest {

    @Mock
    private SchoolBrandingRepository brandingRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private SchoolBrandingService brandingService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getBrandingForSchoolWorksWithoutTenantContext() {
        SchoolBranding branding = new SchoolBranding();
        branding.setId(1L);
        branding.setSchoolId(7L);
        branding.setPrimaryColor("#10b981");

        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.of(branding));

        SchoolBrandingDto dto = brandingService.getBrandingForSchool(7L);

        assertNotNull(dto);
        assertEquals(7L, dto.getSchoolId());
        assertEquals("#10b981", dto.getPrimaryColor());
    }

    @Test
    void updateBrandingForSchoolSavesOnlyRequestedSchool() {
        SchoolBranding branding = new SchoolBranding();
        branding.setId(1L);
        branding.setSchoolId(7L);

        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.of(branding));

        SchoolBrandingDto request = new SchoolBrandingDto();
        request.setPrimaryColor("#10b981");
        request.setSecondaryColor("#16a34a");
        request.setAccentColor("#ea580c");
        request.setBackgroundColor("#f8fafc");
        request.setTextColor("#0f172a");

        SchoolBrandingDto dto = brandingService.updateBrandingForSchool(7L, request);

        assertEquals("#10b981", dto.getPrimaryColor());
        assertEquals("#16a34a", dto.getSecondaryColor());
        verify(brandingRepository).save(branding);
        verify(auditLogService).logForSchool("BRANDING_UPDATE", "SchoolBranding", 1L,
                "Super Admin updated branding for school 7", null, null, null, 7L);
    }

    @Test
    void updateBrandingForSchoolDoesNotTouchOtherSchool() {
        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.empty());
        when(brandingRepository.save(any(SchoolBranding.class))).thenAnswer(inv -> (SchoolBranding) inv.getArgument(0));

        SchoolBrandingDto request = new SchoolBrandingDto();
        request.setPrimaryColor("#10b981");

        brandingService.updateBrandingForSchool(7L, request);

        verify(brandingRepository, never()).findBySchoolId(8L);
    }

    @Test
    void uploadLogoStoresAndSetsLogoUrl() {
        SchoolBranding branding = new SchoolBranding();
        branding.setId(1L);
        branding.setSchoolId(7L);

        String newLogoUrl = "http://localhost/api/uploads/logos/logo-" + UUID.randomUUID() + ".png";
        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.of(branding));
        when(brandingRepository.save(any(SchoolBranding.class))).thenAnswer(inv -> (SchoolBranding) inv.getArgument(0));

        SchoolBrandingDto dto = brandingService.uploadLogo(7L, newLogoUrl);

        assertEquals(newLogoUrl, dto.getLogoUrl());
        verify(auditLogService).logForSchool("LOGO_UPLOAD", "SchoolBranding", 1L,
                "Super Admin uploaded logo for school 7", null, null, null, 7L);
    }

    @Test
    void uploadLogoReplacesPreviousLogoFile() {
        SchoolBranding branding = new SchoolBranding();
        branding.setId(1L);
        branding.setSchoolId(7L);
        branding.setLogoUrl("http://localhost/api/uploads/logos/old.png");

        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.of(branding));
        when(brandingRepository.save(any(SchoolBranding.class))).thenAnswer(inv -> (SchoolBranding) inv.getArgument(0));

        brandingService.uploadLogo(7L, "http://localhost/api/uploads/logos/new.png");

        verify(fileStorageService).deleteLogo("old.png");
    }

    @Test
    void removeLogoClearsLogoAndDeletesFile() {
        SchoolBranding branding = new SchoolBranding();
        branding.setId(1L);
        branding.setSchoolId(7L);
        branding.setLogoUrl("http://localhost/api/uploads/logos/old.png");

        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.of(branding));
        when(brandingRepository.save(any(SchoolBranding.class))).thenAnswer(inv -> (SchoolBranding) inv.getArgument(0));

        SchoolBrandingDto dto = brandingService.removeLogo(7L);

        assertNull(dto.getLogoUrl());
        verify(fileStorageService).deleteLogo("old.png");
        verify(auditLogService).logForSchool("LOGO_REMOVE", "SchoolBranding", 1L,
                "Super Admin removed logo for school 7", null, null, null, 7L);
    }

    @Test
    void resetBrandingRestoresDefaultsAndRemovesLogo() {
        SchoolBranding branding = new SchoolBranding();
        branding.setId(1L);
        branding.setSchoolId(7L);
        branding.setPrimaryColor("#ff0000");
        branding.setLogoUrl("http://localhost/api/uploads/logos/old.png");

        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.of(branding));
        when(brandingRepository.save(any(SchoolBranding.class))).thenAnswer(inv -> (SchoolBranding) inv.getArgument(0));

        SchoolBrandingDto dto = brandingService.resetBranding(7L);

        assertEquals("#3b82f6", dto.getPrimaryColor());
        assertEquals("#64748b", dto.getSecondaryColor());
        assertEquals("#f59e0b", dto.getAccentColor());
        assertEquals("#ffffff", dto.getBackgroundColor());
        assertEquals("#0f172a", dto.getTextColor());
        assertNull(dto.getLogoUrl());
        assertNull(dto.getFaviconUrl());
        verify(fileStorageService).deleteLogo("old.png");
        verify(auditLogService).logForSchool("BRANDING_RESET", "SchoolBranding", 1L,
                "Super Admin reset branding to defaults for school 7", null, null, null, 7L);
    }

    @Test
    void operationsRejectUnknownSchool() {
        when(schoolRepository.existsById(7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> brandingService.getBrandingForSchool(7L));
        assertThrows(ResourceNotFoundException.class,
                () -> brandingService.updateBrandingForSchool(7L, new SchoolBrandingDto()));
        assertThrows(ResourceNotFoundException.class, () -> brandingService.uploadLogo(7L, "url"));
        assertThrows(ResourceNotFoundException.class, () -> brandingService.removeLogo(7L));
        assertThrows(ResourceNotFoundException.class, () -> brandingService.resetBranding(7L));

        verify(brandingRepository, never()).save(any());
    }
}
