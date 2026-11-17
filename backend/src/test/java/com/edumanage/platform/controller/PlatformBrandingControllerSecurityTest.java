package com.edumanage.platform.controller;

import com.edumanage.branding.dto.SchoolBrandingDto;
import com.edumanage.branding.service.SchoolBrandingService;
import com.edumanage.branding.storage.FileStorageService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(PlatformBrandingControllerSecurityTest.TestConfig.class)
class PlatformBrandingControllerSecurityTest {

    @Autowired
    private PlatformBrandingController controller;

    @Autowired
    private SchoolBrandingService brandingService;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @BeforeEach
    void resetState() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        resetMocks();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void resetMocks() {
        reset(brandingService);
        reset(fileStorageService);
        reset(schoolRepository);
    }

    private static SchoolBrandingDto brandingDto(Long schoolId) {
        SchoolBrandingDto dto = new SchoolBrandingDto();
        dto.setId(100L);
        dto.setSchoolId(schoolId);
        dto.setPrimaryColor("#10b981");
        dto.setSecondaryColor("#64748b");
        dto.setAccentColor("#ea580c");
        dto.setBackgroundColor("#ffffff");
        dto.setTextColor("#0f172a");
        dto.setLogoUrl("http://localhost/api/uploads/logos/" + UUID.randomUUID() + ".png");
        return dto;
    }

    private void authenticate(Long schoolId, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(1L, "admin@platform.com", "hash", schoolId, true, authorities),
                "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        if (schoolId != null) {
            TenantContext.setTenantId(schoolId);
        }
    }

    @Test
    void unauthenticatedGetIsRejected() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.getBranding(7L));
        verify(brandingService, never()).getBrandingForSchool(any());
    }

    @Test
    void managerCannotAccessBranding() {
        authenticate(7L, "ROLE_MANAGER");
        assertThrows(AccessDeniedException.class, () -> controller.getBranding(7L));
        verify(brandingService, never()).getBrandingForSchool(any());
    }

    @Test
    void teacherCannotAccessBranding() {
        authenticate(7L, "ROLE_TEACHER");
        assertThrows(AccessDeniedException.class,
                () -> controller.updateBranding(7L, brandingDto(7L)));
        verify(brandingService, never()).updateBrandingForSchool(any(), any());
    }

    @Test
    void superAdminCanGetBrandingForAnySchool() {
        authenticate(null, "ROLE_SUPER_ADMIN");
        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingService.getBrandingForSchool(7L)).thenReturn(brandingDto(7L));

        ResponseEntity<ApiResponse<SchoolBrandingDto>> response = controller.getBranding(7L);

        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().getData().getSchoolId());
        verify(brandingService).getBrandingForSchool(7L);
    }

    @Test
    void superAdminCanUpdateBranding() {
        authenticate(null, "ROLE_SUPER_ADMIN");
        SchoolBrandingDto request = brandingDto(7L);
        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingService.updateBrandingForSchool(7L, request)).thenReturn(brandingDto(7L));

        ResponseEntity<ApiResponse<SchoolBrandingDto>> response = controller.updateBranding(7L, request);

        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().getData().getSchoolId());
        verify(brandingService).updateBrandingForSchool(7L, request);
    }

    @Test
    void superAdminCanUploadLogo() {
        authenticate(null, "ROLE_SUPER_ADMIN");
        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(fileStorageService.storeLogo(any())).thenReturn("a1b2c3.png");
        when(brandingService.uploadLogo(eq(7L), anyString())).thenReturn(brandingDto(7L));

        MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", "logo-content".getBytes());
        jakarta.servlet.http.HttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        ResponseEntity<ApiResponse<SchoolBrandingDto>> response = controller.uploadLogo(7L, file, request);

        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().getData().getSchoolId());
        verify(fileStorageService).storeLogo(file);
        verify(brandingService).uploadLogo(eq(7L), anyString());
    }

    @Test
    void superAdminCanRemoveLogo() {
        authenticate(null, "ROLE_SUPER_ADMIN");
        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingService.removeLogo(7L)).thenReturn(brandingDto(7L));

        ResponseEntity<ApiResponse<SchoolBrandingDto>> response = controller.removeLogo(7L);

        assertNotNull(response.getBody());
        verify(brandingService).removeLogo(7L);
    }

    @Test
    void superAdminCanResetBranding() {
        authenticate(null, "ROLE_SUPER_ADMIN");
        when(schoolRepository.existsById(7L)).thenReturn(true);
        when(brandingService.resetBranding(7L)).thenReturn(brandingDto(7L));

        ResponseEntity<ApiResponse<SchoolBrandingDto>> response = controller.resetBranding(7L);

        assertNotNull(response.getBody());
        verify(brandingService).resetBranding(7L);
    }

    @Test
    void unknownSchoolIsRejected() {
        authenticate(null, "ROLE_SUPER_ADMIN");
        when(schoolRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> controller.getBranding(999L));
        verify(brandingService, never()).getBrandingForSchool(any());
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        SchoolBrandingService brandingService() {
            return mock(SchoolBrandingService.class);
        }

        @Bean
        FileStorageService fileStorageService() {
            return mock(FileStorageService.class);
        }

        @Bean
        SchoolRepository schoolRepository() {
            return mock(SchoolRepository.class);
        }

        @Bean
        PlatformBrandingController platformBrandingController(SchoolBrandingService service,
                                                            FileStorageService storage,
                                                            SchoolRepository schoolRepository) {
            return new PlatformBrandingController(service, storage, schoolRepository);
        }
    }
}
