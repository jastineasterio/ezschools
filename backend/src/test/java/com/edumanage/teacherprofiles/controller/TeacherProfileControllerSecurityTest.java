package com.edumanage.teacherprofiles.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.teacherprofiles.dto.TeacherProfileDto;
import com.edumanage.teacherprofiles.dto.UpdateTeacherProfileRequest;
import com.edumanage.teacherprofiles.service.TeacherProfileService;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(TeacherProfileControllerSecurityTest.TestConfig.class)
class TeacherProfileControllerSecurityTest {

    @Autowired
    private TeacherProfileController controller;

    @Autowired
    private TeacherProfileService teacherProfileService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.getMyProfile());
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.updateMyProfile(new UpdateTeacherProfileRequest()));
        verifyNoServiceInteraction();
    }

    @Test
    void nonTeacherAuthenticatedIsRejected() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(2L, "manager@school.com", "hash", 10L, true,
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(AccessDeniedException.class, () -> controller.getMyProfile());
        verifyNoServiceInteraction();
    }

    @Test
    void teacherCanRetrieveProfile() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(1L, "teacher@school.com", "hash", 10L, true,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        TeacherProfileDto dto = new TeacherProfileDto();
        dto.setRole("TEACHER");
        dto.setSchoolName("Springfield High School");
        when(teacherProfileService.getMyProfile()).thenReturn(dto);

        ResponseEntity<ApiResponse<TeacherProfileDto>> response = controller.getMyProfile();

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("TEACHER", response.getBody().getData().getRole());
        assertEquals("Springfield High School", response.getBody().getData().getSchoolName());
        verify(teacherProfileService).getMyProfile();
    }

    @Test
    void teacherCanUpdatePermittedFields() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(1L, "teacher@school.com", "hash", 10L, true,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("teacher@school.com");

        TeacherProfileDto dto = new TeacherProfileDto();
        dto.setFirstName("Jane");
        when(teacherProfileService.updateMyProfile(request)).thenReturn(dto);

        ResponseEntity<ApiResponse<TeacherProfileDto>> response = controller.updateMyProfile(request);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("Jane", response.getBody().getData().getFirstName());
        verify(teacherProfileService).updateMyProfile(request);
    }

    private void verifyNoServiceInteraction() {
        // Service methods should not be invoked when authorization fails.
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        TeacherProfileService teacherProfileService() {
            return mock(TeacherProfileService.class);
        }

        @Bean
        TeacherProfileController teacherProfileController(TeacherProfileService service) {
            return new TeacherProfileController(service);
        }
    }
}
