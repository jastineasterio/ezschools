package com.edumanage.audit.controller;

import com.edumanage.audit.dto.AuditLogDto;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
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

@SpringJUnitConfig(AuditLogControllerSecurityTest.TestConfig.class)
class AuditLogControllerSecurityTest {

    @Autowired
    private AuditLogController controller;

    @Autowired
    private AuditLogService service;

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
    void unauthenticatedGetAuditLogsIsRejected() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.getAuditLogs(Pageable.unpaged(), null, null, null, null, null, null, null, null));
        verify(service, never()).getAuditLogs(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void managerCannotAccessAuditLogs() {
        authenticate("ROLE_MANAGER");
        assertThrows(AccessDeniedException.class,
                () -> controller.getAuditLogs(Pageable.unpaged(), null, null, null, null, null, null, null, null));
        verify(service, never()).getAuditLogs(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void superAdminCanAccessAuditLogs() {
        authenticate("ROLE_SUPER_ADMIN");
        when(service.getAuditLogs(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new com.edumanage.common.dto.PageResponse<>(List.of(), 0, 20, 0, 0, true));

        controller.getAuditLogs(Pageable.unpaged(), null, null, null, null, null, null, null, null);
        verify(service).getAuditLogs(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void managerCannotAccessAuditOverview() {
        authenticate("ROLE_MANAGER");
        assertThrows(AccessDeniedException.class,
                () -> controller.auditOverview(null, null, null, null, null, null, null, null));
        verify(service, never()).getOverview(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void superAdminCanAccessAuditOverview() {
        authenticate("ROLE_SUPER_ADMIN");
        when(service.getOverview(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new AuditLogService.AuditOverview(0, 0, 0, 0, 0));

        controller.auditOverview(null, null, null, null, null, null, null, null);
        verify(service).getOverview(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void managerCannotAccessAuditLogDetail() {
        authenticate("ROLE_MANAGER");
        assertThrows(AccessDeniedException.class, () -> controller.getAuditLog(1L));
        verify(service, never()).getAuditLog(any());
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        AuditLogService auditLogService() {
            return mock(AuditLogService.class);
        }

        @Bean
        AuditLogController auditLogController(AuditLogService service) {
            return new AuditLogController(service);
        }
    }
}
