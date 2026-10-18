package com.edumanage.schools.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.BadRequestException;
import com.edumanage.schools.dto.CreateSchoolRequest;
import com.edumanage.schools.dto.SchoolDto;
import com.edumanage.schools.dto.UpdateSchoolRequest;
import com.edumanage.schools.service.SchoolService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/schools")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformSchoolController {

    private final SchoolService schoolService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SchoolDto>>> listSchools(Pageable pageable) {
        var schools = schoolService.listSchools(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Schools fetched", schools, null, LocalDateTime.now(), "/api/platform/schools"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchoolDto>> getSchool(@PathVariable Long id) {
        var school = schoolService.getSchool(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "School fetched", school, null, LocalDateTime.now(), "/api/platform/schools/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SchoolDto>> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        var school = schoolService.createSchool(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "School created", school, null, LocalDateTime.now(), "/api/platform/schools"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SchoolDto>> updateSchool(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateSchoolRequest request) {
        var school = schoolService.updateSchool(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "School updated", school, null, LocalDateTime.now(), "/api/platform/schools/" + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SchoolDto>> updateSchoolStatus(@PathVariable Long id,
                                                                     @RequestParam(required = false) String action,
                                                                     @RequestParam(required = false) String reason) {
        if ("suspend".equalsIgnoreCase(action)) {
            var school = schoolService.suspendSchool(id, reason);
            return ResponseEntity.ok(new ApiResponse<>(true, "School suspended", school, null, LocalDateTime.now(), "/api/platform/schools/" + id + "/status"));
        } else if ("activate".equalsIgnoreCase(action)) {
            var school = schoolService.activateSchool(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "School activated", school, null, LocalDateTime.now(), "/api/platform/schools/" + id + "/status"));
        }
        throw new BadRequestException("Invalid action");
    }
}
