package com.edumanage.examinations.controller;

import com.edumanage.examinations.dto.CreateExaminationRequest;
import com.edumanage.examinations.dto.ExaminationDto;
import com.edumanage.examinations.service.ExaminationService;
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
@RequestMapping("/school/examinations")
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HOD')")
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationService examinationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExaminationDto>>> list(Pageable pageable) {
        var examinations = examinationService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examinations fetched", examinations, null, LocalDateTime.now(), "/api/school/examinations"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExaminationDto>> getById(@PathVariable Long id) {
        var examination = examinationService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination fetched", examination, null, LocalDateTime.now(), "/api/school/examinations/" + id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ExaminationDto>> create(@Valid @RequestBody CreateExaminationRequest request) {
        var examination = examinationService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination created", examination, null, LocalDateTime.now(), "/api/school/examinations"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ExaminationDto>> update(@PathVariable Long id,
                                                              @Valid @RequestBody CreateExaminationRequest request) {
        var examination = examinationService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination updated", examination, null, LocalDateTime.now(), "/api/school/examinations/" + id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ExaminationDto>> updateStatus(@PathVariable Long id,
                                                                    @RequestParam String status) {
        var examination = examinationService.updateStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination status updated", examination, null, LocalDateTime.now(), "/api/school/examinations/" + id + "/status"));
    }
}