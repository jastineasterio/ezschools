package com.edumanage.gradingschemes.controller;

import com.edumanage.gradingschemes.dto.CreateGradingSchemeRangeRequest;
import com.edumanage.gradingschemes.dto.CreateGradingSchemeRequest;
import com.edumanage.gradingschemes.dto.GradingSchemeDto;
import com.edumanage.gradingschemes.dto.GradingSchemeRangeDto;
import com.edumanage.gradingschemes.service.GradingSchemeService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/grading-schemes")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class GradingSchemeController {

    private final GradingSchemeService gradingSchemeService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HOD', 'ROLE_TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<GradingSchemeDto>>> list(Pageable pageable) {
        var gradingSchemes = gradingSchemeService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading schemes fetched", gradingSchemes, null, LocalDateTime.now(), "/api/school/grading-schemes"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HOD', 'ROLE_TEACHER')")
    public ResponseEntity<ApiResponse<GradingSchemeDto>> getById(@PathVariable Long id) {
        var gradingScheme = gradingSchemeService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading scheme fetched", gradingScheme, null, LocalDateTime.now(), "/api/school/grading-schemes/" + id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<GradingSchemeDto>> create(@Valid @RequestBody CreateGradingSchemeRequest request) {
        var gradingScheme = gradingSchemeService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading scheme created", gradingScheme, null, LocalDateTime.now(), "/api/school/grading-schemes"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<GradingSchemeDto>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody CreateGradingSchemeRequest request) {
        var gradingScheme = gradingSchemeService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading scheme updated", gradingScheme, null, LocalDateTime.now(), "/api/school/grading-schemes/" + id));
    }

    @PostMapping("/{id}/ranges")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<GradingSchemeRangeDto>> addRange(@PathVariable Long id,
                                                                       @Valid @RequestBody CreateGradingSchemeRangeRequest request) {
        var range = gradingSchemeService.addRange(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading scheme range added", range, null, LocalDateTime.now(), "/api/school/grading-schemes/" + id + "/ranges"));
    }

    @PutMapping("/{id}/ranges/{rangeId}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<GradingSchemeRangeDto>> updateRange(@PathVariable Long id,
                                                                          @PathVariable Long rangeId,
                                                                          @Valid @RequestBody CreateGradingSchemeRangeRequest request) {
        var range = gradingSchemeService.updateRange(id, rangeId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading scheme range updated", range, null, LocalDateTime.now(), "/api/school/grading-schemes/" + id + "/ranges/" + rangeId));
    }

    @DeleteMapping("/{id}/ranges/{rangeId}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteRange(@PathVariable Long id,
                                                         @PathVariable Long rangeId) {
        gradingSchemeService.deleteRange(id, rangeId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading scheme range deleted", null, null, LocalDateTime.now(), "/api/school/grading-schemes/" + id + "/ranges/" + rangeId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<GradingSchemeDto>> toggleStatus(@PathVariable Long id,
                                                                      @RequestParam String status) {
        var gradingScheme = gradingSchemeService.toggleStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading scheme status updated", gradingScheme, null, LocalDateTime.now(), "/api/school/grading-schemes/" + id + "/status"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteScheme(@PathVariable Long id) {
        gradingSchemeService.deleteScheme(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading scheme deleted", null, null, LocalDateTime.now(), "/api/school/grading-schemes/" + id));
    }
}