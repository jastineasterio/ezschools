package com.edumanage.auth.controller;

import com.edumanage.auth.dto.ChangePasswordRequest;
import com.edumanage.auth.dto.LoginResponse;
import com.edumanage.auth.dto.RefreshTokenRequest;
import com.edumanage.auth.dto.TokenResponse;
import com.edumanage.auth.service.AuthService;
import com.edumanage.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest) {
        var response = authService.login(request, httpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", response, null, LocalDateTime.now(), "/api/auth/login"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                              HttpServletRequest httpRequest) {
        var response = authService.refresh(request, httpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token refreshed", response, null, LocalDateTime.now(), "/api/auth/refresh"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody RefreshTokenRequest request,
                                                    HttpServletRequest httpRequest) {
        authService.logout(request.getRefreshToken(), httpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "Logout successful", null, null, LocalDateTime.now(), "/api/auth/logout"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed", null, null, LocalDateTime.now(), "/api/auth/change-password"));
    }
    public ResponseEntity<ApiResponse<com.edumanage.users.dto.UserDto>> me() {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(new ApiResponse<>(true, "Current user", user, null, LocalDateTime.now(), "/api/auth/me"));
    }
}
