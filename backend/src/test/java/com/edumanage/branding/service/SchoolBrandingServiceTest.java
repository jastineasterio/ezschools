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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolBrandingServiceTest {

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
        TenantContext.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getBrandingReturnsExistingBrandingForAssignedSchool() {
        SchoolBranding branding = new SchoolBranding();
        branding.setId(1L);
        branding.setSchoolId(7L);
        branding.setPrimaryColor("#10b981");

        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.of(branding));

        SchoolBrandingDto dto = brandingService.getBranding(7L);

        assertNotNull(dto);
        assertEquals(7L, dto.getSchoolId());
        assertEquals("#10b981", dto.getPrimaryColor());
        verify(brandingRepository, never()).save(any());
    }

    @Test
    void getBrandingCreatesDefaultWhenNoneExists() {
        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingRepository.findBySchoolId(7L)).thenReturn(Optional.empty());

        SchoolBranding defaultBranding = new SchoolBranding();
        defaultBranding.setId(2L);
        defaultBranding.setSchoolId(7L);
        defaultBranding.setPrimaryColor("#3b82f6");
        when(brandingRepository.save(any(SchoolBranding.class))).thenReturn(defaultBranding);

        SchoolBrandingDto dto = brandingService.getBranding(7L);

        assertNotNull(dto);
        assertEquals("#3b82f6", dto.getPrimaryColor());
        assertEquals("#64748b", dto.getSecondaryColor());
        verify(brandingRepository).save(any(SchoolBranding.class));
    }

    @Test
    void getBrandingRejectsUnknownSchool() {
        when(schoolRepository.existsById(7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> brandingService.getBranding(7L));
        verify(brandingRepository, never()).save(any());
    }

    @Test
    void updateBrandingSavesOnlyAssignedSchool() {
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

        SchoolBrandingDto dto = brandingService.updateBranding(7L, request);

        assertEquals("#10b981", dto.getPrimaryColor());
        assertEquals("#16a34a", dto.getSecondaryColor());
        assertEquals("#ea580c", dto.getAccentColor());
        assertEquals("#f8fafc", dto.getBackgroundColor());
        assertEquals("#0f172a", dto.getTextColor());
        verify(brandingRepository).save(branding);
        verify(auditLogService).log("BRANDING_UPDATE", "SchoolBranding", 1L,
                "Updated branding for school 7", null, null, null);
    }

    @Test
    void updateBrandingRejectsOtherSchool() {
        SchoolBrandingDto request = new SchoolBrandingDto();
        request.setPrimaryColor("#10b981");

        assertThrows(ForbiddenException.class, () -> brandingService.updateBranding(9L, request));
        verify(brandingRepository, never()).save(any());
    }

    @Test
    void updateBrandingRejectsUnknownSchool() {
        when(schoolRepository.existsById(7L)).thenReturn(false);

        SchoolBrandingDto request = new SchoolBrandingDto();
        request.setPrimaryColor("#10b981");

        assertThrows(ResourceNotFoundException.class, () -> brandingService.updateBranding(7L, request));
        verify(brandingRepository, never()).save(any());
    }
}
