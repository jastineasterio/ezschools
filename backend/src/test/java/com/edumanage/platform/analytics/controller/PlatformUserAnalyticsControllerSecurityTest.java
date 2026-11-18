package com.edumanage.platform.analytics.controller;

import com.edumanage.platform.analytics.dto.PackageUserDistributionDto;
import com.edumanage.platform.analytics.dto.RoleDistributionDto;
import com.edumanage.platform.analytics.dto.SchoolUserDistributionDto;
import com.edumanage.platform.analytics.dto.TrendDto;
import com.edumanage.platform.analytics.dto.UserOverviewDto;
import com.edumanage.platform.analytics.service.PlatformUserAnalyticsService;
import com.edumanage.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(PlatformUserAnalyticsControllerSecurityTest.TestConfig.class)
class PlatformUserAnalyticsControllerSecurityTest {

    @Autowired
    private PlatformUserAnalyticsController controller;

    @Autowired
    private PlatformUserAnalyticsService service;

    @BeforeEach
    void resetState() {
        SecurityContextHolder.clearContext();
        reset(service);
    }

    private void authenticate(String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(1L, "admin@platform.com", "hash", null, true, authorities),
                "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void unauthenticatedOverviewIsRejected() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.overview(LocalDateTime.now().minusDays(30), LocalDateTime.now()));
        verify(service, never()).getOverview(any(), any());
    }

    @Test
    void managerCannotAccessOverview() {
        authenticate("ROLE_MANAGER");
        assertThrows(AccessDeniedException.class,
                () -> controller.overview(LocalDateTime.now().minusDays(30), LocalDateTime.now()));
        verify(service, never()).getOverview(any(), any());
    }

    @Test
    void superAdminCanAccessOverview() {
        authenticate("ROLE_SUPER_ADMIN");
        when(service.getOverview(any(), any())).thenReturn(new UserOverviewDto());

        ResponseEntity<?> response = controller.overview(LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assert response != null;
        assert response.getBody() != null;
        verify(service).getOverview(any(), any());
    }

    @Test
    void superAdminCanAccessSchools() {
        authenticate("ROLE_SUPER_ADMIN");
        when(service.getSchoolDistribution(any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        controller.schools(LocalDateTime.now().minusDays(30), LocalDateTime.now(), null,
                org.springframework.data.domain.Pageable.unpaged());
        verify(service).getSchoolDistribution(any(), any(), any(), any());
    }

    @Test
    void managerCannotAccessPackages() {
        authenticate("ROLE_MANAGER");
        assertThrows(AccessDeniedException.class,
                () -> controller.packages(LocalDateTime.now().minusDays(30), LocalDateTime.now()));
        verify(service, never()).getPackageDistribution(any(), any());
    }

    @Test
    void superAdminCanAccessPackages() {
        authenticate("ROLE_SUPER_ADMIN");
        when(service.getPackageDistribution(any(), any())).thenReturn(List.of());

        controller.packages(LocalDateTime.now().minusDays(30), LocalDateTime.now());
        verify(service).getPackageDistribution(any(), any());
    }

    @Test
    void unauthenticatedTrendsIsRejected() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.trends(LocalDateTime.now().minusDays(30), LocalDateTime.now(), "day"));
        verify(service, never()).getTrends(any(), any(), any());
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        PlatformUserAnalyticsService platformUserAnalyticsService() {
            return mock(PlatformUserAnalyticsService.class);
        }

        @Bean
        PlatformUserAnalyticsController platformUserAnalyticsController(PlatformUserAnalyticsService service) {
            return new PlatformUserAnalyticsController(service);
        }
    }
}
