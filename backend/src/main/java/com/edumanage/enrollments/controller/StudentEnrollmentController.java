package com.edumanage.enrollments.controller;

import com.edumanage.enrollments.dto.CreateStudentEnrollmentRequest;
import com.edumanage.enrollments.dto.StudentEnrollmentDto;
import com.edumanage.enrollments.service.StudentEnrollmentService;
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
@RequestMapping("/school/enrollments")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class StudentEnrollmentController {

    private final StudentEnrollmentService studentEnrollmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<StudentEnrollmentDto>>> list(Pageable pageable) {
        var enrollments = studentEnrollmentService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrollments fetched", enrollments, null, LocalDateTime.now(), "/api/school/enrollments"));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<java.util.List<StudentEnrollmentDto>>> listByStudent(@PathVariable Long studentId) {
        var enrollments = studentEnrollmentService.listByStudent(studentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrollments fetched by student", enrollments, null, LocalDateTime.now(), "/api/school/enrollments/student/" + studentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<StudentEnrollmentDto>> getById(@PathVariable Long id) {
        var enrollment = studentEnrollmentService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrollment fetched", enrollment, null, LocalDateTime.now(), "/api/school/enrollments/" + id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<StudentEnrollmentDto>> create(@Valid @RequestBody CreateStudentEnrollmentRequest request) {
        var enrollment = studentEnrollmentService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrollment created", enrollment, null, LocalDateTime.now(), "/api/school/enrollments"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<StudentEnrollmentDto>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody CreateStudentEnrollmentRequest request) {
        var enrollment = studentEnrollmentService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrollment updated", enrollment, null, LocalDateTime.now(), "/api/school/enrollments/" + id));
    }
}