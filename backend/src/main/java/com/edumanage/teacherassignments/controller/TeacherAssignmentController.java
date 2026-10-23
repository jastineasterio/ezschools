package com.edumanage.teacherassignments.controller;

import com.edumanage.teacherassignments.dto.CreateTeacherAssignmentRequest;
import com.edumanage.teacherassignments.dto.CreateTeacherAssignmentBatchRequest;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teacherassignments.service.TeacherAssignmentService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.security.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<TeacherAssignmentDto>>> list(Pageable pageable) {
        var assignments = teacherAssignmentService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignments fetched", assignments, null, LocalDateTime.now(), "/api/school/teacher-assignments"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<TeacherAssignmentDto>> getById(@PathVariable Long id) {
        var assignment = teacherAssignmentService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignment fetched", assignment, null, LocalDateTime.now(), "/api/school/teacher-assignments/" + id));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_TEACHER')")
    public ResponseEntity<ApiResponse<java.util.List<TeacherAssignmentDto>>> listByTeacher(@PathVariable Long teacherId) {
        var currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.hasRole(currentUser, "TEACHER")
                && !currentUser.getId().equals(teacherId)) {
            throw new ForbiddenException("Teachers may only view their own assignments");
        }
        var assignments = teacherAssignmentService.listByTeacher(teacherId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignments fetched", assignments, null, LocalDateTime.now(), "/api/school/teacher-assignments/teacher/" + teacherId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<TeacherAssignmentDto>> create(@Valid @RequestBody CreateTeacherAssignmentRequest request) {
        var assignment = teacherAssignmentService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignment created", assignment, null, LocalDateTime.now(), "/api/school/teacher-assignments"));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<TeacherAssignmentDto>>> createBatch(
            @Valid @RequestBody CreateTeacherAssignmentBatchRequest request) {
        var assignments = teacherAssignmentService.createBatch(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignments created", assignments, null,
                LocalDateTime.now(), "/api/school/teacher-assignments/batch"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<TeacherAssignmentDto>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody CreateTeacherAssignmentRequest request) {
        var assignment = teacherAssignmentService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignment updated", assignment, null, LocalDateTime.now(), "/api/school/teacher-assignments/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teacherAssignmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher assignment deleted", null, null, LocalDateTime.now(), "/api/school/teacher-assignments/" + id));
    }
}