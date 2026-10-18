package com.edumanage.examinationtypes.controller;

import com.edumanage.examinationtypes.dto.CreateExaminationTypeRequest;
import com.edumanage.examinationtypes.dto.ExaminationTypeDto;
import com.edumanage.examinationtypes.service.ExaminationTypeService;
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
@RequestMapping("/school/examination-types")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ExaminationTypeController {

    private final ExaminationTypeService examinationTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExaminationTypeDto>>> list(Pageable pageable) {
        var examinationTypes = examinationTypeService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination types fetched", examinationTypes, null, LocalDateTime.now(), "/api/school/examination-types"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExaminationTypeDto>> getById(@PathVariable Long id) {
        var examinationType = examinationTypeService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination type fetched", examinationType, null, LocalDateTime.now(), "/api/school/examination-types/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExaminationTypeDto>> create(@Valid @RequestBody CreateExaminationTypeRequest request) {
        var examinationType = examinationTypeService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination type created", examinationType, null, LocalDateTime.now(), "/api/school/examination-types"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExaminationTypeDto>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody CreateExaminationTypeRequest request) {
        var examinationType = examinationTypeService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination type updated", examinationType, null, LocalDateTime.now(), "/api/school/examination-types/" + id));
    }
}