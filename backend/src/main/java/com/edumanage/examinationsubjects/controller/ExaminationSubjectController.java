package com.edumanage.examinationsubjects.controller;

import com.edumanage.examinationsubjects.dto.CreateExaminationSubjectRequest;
import com.edumanage.examinationsubjects.dto.ExaminationSubjectDto;
import com.edumanage.examinationsubjects.service.ExaminationSubjectService;
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
@RequestMapping("/school/examinations/{examinationId}/subjects")
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HOD')")
@RequiredArgsConstructor
public class ExaminationSubjectController {

    private final ExaminationSubjectService examinationSubjectService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExaminationSubjectDto>>> listByExamination(@PathVariable Long examinationId,
                                                                                              Pageable pageable) {
        var examinationSubjects = examinationSubjectService.listByExamination(examinationId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination subjects fetched", examinationSubjects, null, LocalDateTime.now(), "/api/school/examinations/" + examinationId + "/subjects"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<java.util.List<ExaminationSubjectDto>>> listAllByExamination(@PathVariable Long examinationId) {
        var examinationSubjects = examinationSubjectService.listAllByExamination(examinationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination subjects fetched", examinationSubjects, null, LocalDateTime.now(), "/api/school/examinations/" + examinationId + "/subjects/all"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExaminationSubjectDto>> create(@PathVariable Long examinationId,
                                                                     @Valid @RequestBody CreateExaminationSubjectRequest request) {
        var examinationSubject = examinationSubjectService.create(examinationId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination subject created", examinationSubject, null, LocalDateTime.now(), "/api/school/examinations/" + examinationId + "/subjects"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExaminationSubjectDto>> update(@PathVariable Long examinationId,
                                                                     @PathVariable Long id,
                                                                     @Valid @RequestBody CreateExaminationSubjectRequest request) {
        var examinationSubject = examinationSubjectService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination subject updated", examinationSubject, null, LocalDateTime.now(), "/api/school/examinations/" + examinationId + "/subjects/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long examinationId,
                                                    @PathVariable Long id) {
        examinationSubjectService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination subject deleted", null, null, LocalDateTime.now(), "/api/school/examinations/" + examinationId + "/subjects/" + id));
    }
}