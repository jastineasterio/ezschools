package com.edumanage.teacherassignments.controller;

import com.edumanage.teacherassignments.dto.CreateTeacherAssignmentRequest;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teacherassignments.service.TeacherAssignmentService;
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
@RequestMapping("/school/teacher-assignments")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService teacherAssignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TeacherAssignmentDto>>> list(Pageable pageable) {
        var assignments = teacherAssignmentService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignments fetched", assignments, null, LocalDateTime.now(), "/api/school/teacher-assignments"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherAssignmentDto>> getById(@PathVariable Long id) {
        var assignment = teacherAssignmentService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignment fetched", assignment, null, LocalDateTime.now(), "/api/school/teacher-assignments/" + id));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<java.util.List<TeacherAssignmentDto>>> listByTeacher(@PathVariable Long teacherId) {
        var assignments = teacherAssignmentService.listByTeacher(teacherId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignments fetched", assignments, null, LocalDateTime.now(), "/api/school/teacher-assignments/teacher/" + teacherId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeacherAssignmentDto>> create(@Valid @RequestBody CreateTeacherAssignmentRequest request) {
        var assignment = teacherAssignmentService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignment created", assignment, null, LocalDateTime.now(), "/api/school/teacher-assignments"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherAssignmentDto>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody CreateTeacherAssignmentRequest request) {
        var assignment = teacherAssignmentService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignment updated", assignment, null, LocalDateTime.now(), "/api/school/teacher-assignments/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teacherAssignmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignment deleted", null, null, LocalDateTime.now(), "/api/school/teacher-assignments/" + id));
    }
}