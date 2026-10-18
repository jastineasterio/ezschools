package com.edumanage.schools.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.schools.dto.SchoolDto;
import com.edumanage.schools.service.SchoolService;
import com.edumanage.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<SchoolDto>> getProfile() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("School users only");
        }
        var school = schoolService.getSchool(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "School profile fetched", school, null, LocalDateTime.now(), "/api/school/profile"));
    }
}
