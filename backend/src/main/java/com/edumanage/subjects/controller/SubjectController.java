package com.edumanage.subjects.controller;

import com.edumanage.subjects.dto.CreateSubjectRequest;
import com.edumanage.subjects.dto.SubjectDto;
import com.edumanage.subjects.service.SubjectService;
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
@RequestMapping("/school/subjects")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SubjectDto>>> list(Pageable pageable) {
        var subjects = subjectService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subjects fetched", subjects, null, LocalDateTime.now(), "/api/school/subjects"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectDto>> getById(@PathVariable Long id) {
        var subject = subjectService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subject fetched", subject, null, LocalDateTime.now(), "/api/school/subjects/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubjectDto>> create(@Valid @RequestBody CreateSubjectRequest request) {
        var subject = subjectService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subject created", subject, null, LocalDateTime.now(), "/api/school/subjects"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectDto>> update(@PathVariable Long id,
                                                          @Valid @RequestBody CreateSubjectRequest request) {
        var subject = subjectService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subject updated", subject, null, LocalDateTime.now(), "/api/school/subjects/" + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SubjectDto>> updateStatus(@PathVariable Long id,
                                                                @RequestParam String status) {
        var subject = subjectService.updateStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subject status updated", subject, null, LocalDateTime.now(), "/api/school/subjects/" + id + "/status"));
    }
}