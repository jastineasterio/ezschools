package com.edumanage.auth.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.util.AuditAware;
import com.edumanage.common.util.TenantAuditor;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.exception.UnauthorizedException;
import com.edumanage.roles.entity.Role;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.security.JwtService;
import com.edumanage.users.dto.UserDto;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.auth.dto.LoginRequest;
import com.edumanage.auth.dto.LoginResponse;
import com.edumanage.auth.dto.RefreshTokenRequest;
import com.edumanage.auth.dto.TokenResponse;
import com.edumanage.auth.entity.RefreshToken;
import com.edumanage.auth.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final RoleRepository roleRepository;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService, PasswordEncoder passwordEncoder, AuditLogService auditLogService,
                       RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        var user = userRepository.findWithRolesByEmail(request.getEmail())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        var userDetails = CustomUserDetails.from(user, roles);
        var accessToken = jwtService.generateAccessToken(userDetails, user.getId(), user.getSchoolId(), roles);
        var refreshTokenStr = jwtService.generateRefreshToken(userDetails);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(7 * 24 * 60 * 60));
        refreshTokenRepository.save(refreshToken);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        auditLogService.log("LOGIN", "Auth", user.getId(), "User logged in",
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);

        return new LoginResponse(
                accessToken,
                refreshTokenStr,
                "Bearer",
                900L,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getSchoolId() != null ? String.valueOf(user.getSchoolId()) : null
        );
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        var refreshToken = refreshTokenRepository.findByTokenAndRevokedFalseAndExpiresAtAfter(
                        request.getRefreshToken(), LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        var user = refreshToken.getUser();
        var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        var userDetails = CustomUserDetails.from(user, roles);
        var newAccessToken = jwtService.generateAccessToken(userDetails, user.getId(), user.getSchoolId(), roles);

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        var newRefreshTokenStr = jwtService.generateRefreshToken(userDetails);
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setToken(newRefreshTokenStr);
        newRefreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(7 * 24 * 60 * 60));
        refreshTokenRepository.save(newRefreshToken);

        auditLogService.log("REFRESH", "Auth", user.getId(), "Token refreshed",
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);

        return new TokenResponse(newAccessToken, newRefreshTokenStr, "Bearer", 900L);
    }

    @Transactional
    public void logout(String refreshTokenStr, HttpServletRequest httpRequest) {
        var refreshToken = refreshTokenRepository.findByToken(refreshTokenStr).orElse(null);
        if (refreshToken != null) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            auditLogService.log("LOGOUT", "Auth", refreshToken.getUser().getId(), "User logged out",
                    httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);
        }
    }

    public UserDto getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        var email = auth.getName();
        var user = userRepository.findWithRolesByEmail(email)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDto(user);
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUuid(user.getUuid() != null ? user.getUuid().toString() : null);
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setSchoolId(user.getSchoolId());
        dto.setActive(user.isActive());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());

        dto.setRoles(
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toList())
        );

        return dto;
    }
}

