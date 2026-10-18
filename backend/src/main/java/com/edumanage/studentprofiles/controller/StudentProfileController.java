package com.edumanage.studentprofiles.controller;

import com.edumanage.studentprofiles.dto.CreateStudentProfileRequest;
import com.edumanage.studentprofiles.dto.StudentProfileDto;
import com.edumanage.studentprofiles.service.StudentProfileService;
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
@RequestMapping("/school/students")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudentProfileDto>>> list(Pageable pageable) {
        var students = studentProfileService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Students fetched", students, null, LocalDateTime.now(), "/api/school/students"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentProfileDto>> getById(@PathVariable Long id) {
        var student = studentProfileService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student fetched", student, null, LocalDateTime.now(), "/api/school/students/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentProfileDto>> create(@Valid @RequestBody CreateStudentProfileRequest request) {
        var student = studentProfileService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student created", student, null, LocalDateTime.now(), "/api/school/students"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentProfileDto>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody CreateStudentProfileRequest request) {
        var student = studentProfileService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student updated", student, null, LocalDateTime.now(), "/api/school/students/" + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<StudentProfileDto>> updateStatus(@PathVariable Long id,
                                                                       @RequestParam String status) {
        var student = studentProfileService.updateStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student status updated", student, null, LocalDateTime.now(), "/api/school/students/" + id + "/status"));
    }
}