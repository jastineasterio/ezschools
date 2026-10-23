package com.edumanage.schools.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.schools.dto.SchoolDto;
import com.edumanage.schools.service.SchoolService;
import com.edumanage.teachertimetables.dto.UpdateSchoolTimeZoneRequest;
import com.edumanage.tenant.TenantContext;
import jakarta.validation.Valid;
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

    @PatchMapping("/profile/time-zone")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<SchoolDto>> updateTimeZone(
            @Valid @RequestBody UpdateSchoolTimeZoneRequest request) {
        var school = schoolService.updateTimeZone(request.getTimeZone());
        return ResponseEntity.ok(new ApiResponse<>(true, "School time zone updated", school, null,
                LocalDateTime.now(), "/api/school/profile/time-zone"));
    }
}
