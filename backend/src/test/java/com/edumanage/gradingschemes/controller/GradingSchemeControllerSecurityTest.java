package com.edumanage.gradingschemes.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.gradingschemes.dto.CreateGradingSchemeRequest;
import com.edumanage.gradingschemes.dto.GradingSchemeDto;
import com.edumanage.gradingschemes.service.GradingSchemeService;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(GradingSchemeControllerSecurityTest.TestConfig.class)
class GradingSchemeControllerSecurityTest {

    @Autowired
    private GradingSchemeController controller;

    @Autowired
    private GradingSchemeService gradingSchemeService;

    @BeforeEach
    void resetState() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        reset(gradingSchemeService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticate(Long schoolId, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(1L, "manager@school.com", "hash", schoolId, true, authorities),
                "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        if (schoolId != null) {
            TenantContext.setTenantId(schoolId);
        }
    }

    @Test
    void unauthenticatedListIsRejected() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.list(null));
        verify(gradingSchemeService, never()).list(any());
    }

    @Test
    void unauthenticatedCreateIsRejected() {
        CreateGradingSchemeRequest request = new CreateGradingSchemeRequest("Scheme", "desc", false, "ACTIVE");
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.create(request));
        verify(gradingSchemeService, never()).create(any());
    }

    @Test
    void studentIsRejectedFromList() {
        authenticate(7L, "ROLE_STUDENT");
        assertThrows(AccessDeniedException.class, () -> controller.list(null));
        verify(gradingSchemeService, never()).list(any());
    }

    @Test
    void managerCanListOwnSchoolSchemes() {
        authenticate(7L, "ROLE_MANAGER");
        GradingSchemeDto dto = new GradingSchemeDto();
        dto.setId(60L);
        dto.setName("Scheme");
        dto.setSchoolId(7L);
        PageResponse<GradingSchemeDto> page = new PageResponse<>(List.of(dto), 0, 1, 1, 1, false);
        when(gradingSchemeService.list(any())).thenReturn(page);

        ResponseEntity<ApiResponse<PageResponse<GradingSchemeDto>>> response = controller.list(null);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().getData().getContent().get(0).getSchoolId());
        verify(gradingSchemeService, atLeastOnce()).list(any());
    }

    @Test
    void hodAndTeacherCanList() {
        for (String role : new String[]{"ROLE_HOD", "ROLE_TEACHER"}) {
            authenticate(7L, role);
            GradingSchemeDto dto = new GradingSchemeDto();
            dto.setId(60L);
            dto.setSchoolId(7L);
            PageResponse<GradingSchemeDto> page = new PageResponse<>(List.of(dto), 0, 1, 1, 1, false);
            when(gradingSchemeService.list(any())).thenReturn(page);

            ResponseEntity<ApiResponse<PageResponse<GradingSchemeDto>>> response = controller.list(null);

            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getData().getContent().size());
            verify(gradingSchemeService, atLeastOnce()).list(any());
        }
    }

    @Test
    void managerCanCreateScheme() {
        authenticate(7L, "ROLE_MANAGER");
        CreateGradingSchemeRequest request = new CreateGradingSchemeRequest("Scheme", "desc", false, "ACTIVE");
        GradingSchemeDto dto = new GradingSchemeDto();
        dto.setId(60L);
        dto.setName("Scheme");
        dto.setSchoolId(7L);
        when(gradingSchemeService.create(any())).thenReturn(dto);

        ResponseEntity<ApiResponse<GradingSchemeDto>> response = controller.create(request);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(60L, response.getBody().getData().getId());
        assertEquals(7L, response.getBody().getData().getSchoolId());
        verify(gradingSchemeService, atLeastOnce()).create(any());
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        GradingSchemeService gradingSchemeService() {
            return mock(GradingSchemeService.class);
        }

        @Bean
        GradingSchemeController gradingSchemeController(GradingSchemeService service) {
            return new GradingSchemeController(service);
        }
    }
}
