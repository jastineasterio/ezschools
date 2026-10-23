package com.edumanage.academicyears.controller;

import com.edumanage.academicyears.dto.AcademicYearDto;
import com.edumanage.academicyears.dto.CreateAcademicYearRequest;
import com.edumanage.academicyears.service.AcademicYearService;
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
@RequestMapping("/school/academic-years")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AcademicYearDto>>> listBySchool(
            Pageable pageable, @RequestParam(required = false) String search) {
        var academicYears = academicYearService.listBySchool(pageable, search);
        return ResponseEntity.ok(new ApiResponse<>(true, "Academic years fetched", academicYears, null, LocalDateTime.now(), "/api/school/academic-years"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AcademicYearDto>> getById(@PathVariable Long id) {
        var academicYear = academicYearService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Academic year fetched", academicYear, null, LocalDateTime.now(), "/api/school/academic-years/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AcademicYearDto>> create(@Valid @RequestBody CreateAcademicYearRequest request) {
        var academicYear = academicYearService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Academic year created", academicYear, null, LocalDateTime.now(), "/api/school/academic-years"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AcademicYearDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody CreateAcademicYearRequest request) {
        var academicYear = academicYearService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Academic year updated", academicYear, null, LocalDateTime.now(), "/api/school/academic-years/" + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AcademicYearDto>> setCurrent(@PathVariable Long id) {
        var academicYear = academicYearService.setCurrent(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Academic year status updated", academicYear, null, LocalDateTime.now(), "/api/school/academic-years/" + id + "/status"));
    }
}