package com.edumanage.results.controller;

import com.edumanage.results.dto.CreateResultRequest;
import com.edumanage.results.dto.ResultDto;
import com.edumanage.results.service.ResultService;
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
@RequestMapping("/school/results")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ResultDto>>> list(Pageable pageable) {
        var results = resultService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Results fetched", results, null, LocalDateTime.now(), "/api/school/results"));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<PageResponse<ResultDto>>> listByStudent(@PathVariable Long studentId,
                                                                              Pageable pageable) {
        var results = resultService.listByStudent(studentId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Results fetched by student", results, null, LocalDateTime.now(), "/api/school/results/student/" + studentId));
    }

    @GetMapping("/examination/{examinationId}")
    public ResponseEntity<ApiResponse<PageResponse<ResultDto>>> listByExamination(@PathVariable Long examinationId,
                                                                                  Pageable pageable) {
        var results = resultService.listByExamination(examinationId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Results fetched by examination", results, null, LocalDateTime.now(), "/api/school/results/examination/" + examinationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResultDto>> getById(@PathVariable Long id) {
        var result = resultService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result fetched", result, null, LocalDateTime.now(), "/api/school/results/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResultDto>> create(@Valid @RequestBody CreateResultRequest request) {
        var result = resultService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result created", result, null, LocalDateTime.now(), "/api/school/results"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResultDto>> update(@PathVariable Long id,
                                                         @Valid @RequestBody CreateResultRequest request) {
        var result = resultService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result updated", result, null, LocalDateTime.now(), "/api/school/results/" + id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ResultDto>> submit(@PathVariable Long id) {
        var result = resultService.submit(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result submitted", result, null, LocalDateTime.now(), "/api/school/results/" + id + "/submit"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ResultDto>> approve(@PathVariable Long id) {
        var result = resultService.approve(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result approved", result, null, LocalDateTime.now(), "/api/school/results/" + id + "/approve"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ResultDto>> reject(@PathVariable Long id,
                                                         @RequestParam(required = false) String comment) {
        var result = resultService.reject(id, comment);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result rejected", result, null, LocalDateTime.now(), "/api/school/results/" + id + "/reject"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ResultDto>> publish(@PathVariable Long id) {
        var result = resultService.publish(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result published", result, null, LocalDateTime.now(), "/api/school/results/" + id + "/publish"));
    }
}