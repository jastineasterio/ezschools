package com.edumanage.branding.controller;

import com.edumanage.branding.dto.SchoolBrandingDto;
import com.edumanage.branding.service.SchoolBrandingService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/branding")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SchoolBrandingController {

    private final SchoolBrandingService brandingService;

    @GetMapping
    public ResponseEntity<ApiResponse<SchoolBrandingDto>> getBranding() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("School users only");
        }
        var branding = brandingService.getBranding(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Branding fetched", branding, null, LocalDateTime.now(), "/api/school/branding"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SchoolBrandingDto>> updateBranding(@Valid @RequestBody SchoolBrandingDto request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("School users only");
        }
        var branding = brandingService.updateBranding(tenantId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Branding updated", branding, null, LocalDateTime.now(), "/api/school/branding"));
    }
}
