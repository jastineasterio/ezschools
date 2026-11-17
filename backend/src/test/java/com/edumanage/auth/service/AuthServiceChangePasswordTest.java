package com.edumanage.auth.service;

import com.edumanage.auth.repository.RefreshTokenRepository;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.exception.UnauthorizedException;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.roles.entity.Role;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.audit.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceChangePasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditLogService auditLogService;

    private AuthService service;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        auditLogService = mock(AuditLogService.class);
        passwordEncoder = new BCryptPasswordEncoder();

        service = new AuthService(
                userRepository,
                refreshTokenRepository,
                mock(com.edumanage.security.JwtService.class),
                passwordEncoder,
                auditLogService,
                mock(com.edumanage.roles.repository.RoleRepository.class));

        CustomUserDetails principal = new CustomUserDetails(
                1L,
                "teacher@school.com",
                "hash",
                10L,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User userWithPassword(String password) {
        User u = new User();
        u.setId(1L);
        u.setEmail("teacher@school.com");
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setActive(true);
        Role r = new Role();
        r.setName("TEACHER");
        u.getRoles().add(r);
        return u;
    }

    @Test
    void changePasswordWithCorrectCurrentPassword() {
        User user = userWithPassword("oldPassword");
        when(userRepository.findWithRolesByEmail("teacher@school.com")).thenReturn(Optional.of(user));

        com.edumanage.auth.dto.ChangePasswordRequest request = new com.edumanage.auth.dto.ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("NewPass123!");
        request.setConfirmPassword("NewPass123!");
        service.changePassword(request);

        assertTrue(passwordEncoder.matches("NewPass123!", user.getPasswordHash()));
        assertFalse(passwordEncoder.matches("oldPassword", user.getPasswordHash()));
    }

    @Test
    void incorrectCurrentPasswordIsRejected() {
        User user = userWithPassword("oldPassword");
        when(userRepository.findWithRolesByEmail("teacher@school.com")).thenReturn(Optional.of(user));

        com.edumanage.auth.dto.ChangePasswordRequest request = new com.edumanage.auth.dto.ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("NewPass123!");
        request.setConfirmPassword("NewPass123!");

        assertThrows(ForbiddenException.class, () -> service.changePassword(request));
    }

    @Test
    void passwordConfirmationMismatchIsRejected() {
        User user = userWithPassword("oldPassword");
        when(userRepository.findWithRolesByEmail("teacher@school.com")).thenReturn(Optional.of(user));

        com.edumanage.auth.dto.ChangePasswordRequest request = new com.edumanage.auth.dto.ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("NewPass123!");
        request.setConfirmPassword("DifferentPass123!");

        assertThrows(BadRequestException.class, () -> service.changePassword(request));
    }

    @Test
    void unauthenticatedChangePasswordIsRejected() {
        SecurityContextHolder.clearContext();
        com.edumanage.auth.dto.ChangePasswordRequest request = new com.edumanage.auth.dto.ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("NewPass123!");
        request.setConfirmPassword("NewPass123!");
        assertThrows(UnauthorizedException.class, () -> service.changePassword(request));
    }

    @Test
    void userNotFoundIsRejected() {
        when(userRepository.findWithRolesByEmail("teacher@school.com")).thenReturn(Optional.empty());
        com.edumanage.auth.dto.ChangePasswordRequest request = new com.edumanage.auth.dto.ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("NewPass123!");
        request.setConfirmPassword("NewPass123!");
        assertThrows(ResourceNotFoundException.class, () -> service.changePassword(request));
    }
}
