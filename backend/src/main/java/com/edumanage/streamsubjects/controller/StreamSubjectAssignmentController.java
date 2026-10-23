package com.edumanage.streamsubjects.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.streamsubjects.dto.SaveStreamSubjectsRequest;
import com.edumanage.streamsubjects.dto.StreamSubjectAssignmentDto;
import com.edumanage.streamsubjects.service.StreamSubjectAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/stream-subject-assignments")
@RequiredArgsConstructor
public class StreamSubjectAssignmentController {

    private final StreamSubjectAssignmentService assignmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<StreamSubjectAssignmentDto>>> list(
            @RequestParam Long academicYearId, @RequestParam Long classId, @RequestParam Long streamId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Stream subjects fetched",
                assignmentService.list(academicYearId, classId, streamId), null, LocalDateTime.now(),
                "/api/school/stream-subject-assignments"));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<StreamSubjectAssignmentDto>>> save(
            @Valid @RequestBody SaveStreamSubjectsRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Stream subjects saved",
                assignmentService.save(request), null, LocalDateTime.now(),
                "/api/school/stream-subject-assignments"));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StreamSubjectAssignmentDto>>> subjectsForStudent(
            @PathVariable Long studentId, @RequestParam Long academicYearId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Student subjects fetched",
                assignmentService.subjectsForStudent(studentId, academicYearId), null, LocalDateTime.now(),
                "/api/school/stream-subject-assignments/student/" + studentId));
    }
}
