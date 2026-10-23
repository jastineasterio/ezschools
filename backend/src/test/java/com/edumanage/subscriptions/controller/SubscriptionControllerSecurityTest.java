package com.edumanage.subscriptions.controller;

import com.edumanage.security.CustomUserDetails;
import com.edumanage.subscriptions.dto.AssignPackageRequest;
import com.edumanage.subscriptions.service.SubscriptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(SubscriptionControllerSecurityTest.TestConfig.class)
class SubscriptionControllerSecurityTest {

    @Autowired
    private SubscriptionController controller;

    @Autowired
    private SubscriptionService subscriptionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void schoolManagerCannotAssignPackageToAnySchool() {
        var managerAuthority = new SimpleGrantedAuthority("ROLE_MANAGER");
        var manager = new CustomUserDetails(
                7L,
                "manager@example.com",
                "password",
                10L,
                true,
                List.of(managerAuthority));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager, null, List.of(managerAuthority)));

        assertThrows(AccessDeniedException.class,
                () -> controller.assignPackage(10L, new AssignPackageRequest(20L, null)));
        verifyNoInteractions(subscriptionService);
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        SubscriptionService subscriptionService() {
            return mock(SubscriptionService.class);
        }

        @Bean
        SubscriptionController subscriptionController(SubscriptionService subscriptionService) {
            return new SubscriptionController(subscriptionService);
        }
    }
}
