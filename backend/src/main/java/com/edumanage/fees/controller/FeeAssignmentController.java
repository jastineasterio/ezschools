package com.edumanage.fees.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.fees.dto.BulkFeeAssignmentPreviewDto;
import com.edumanage.fees.dto.BulkFeeAssignmentRequest;
import com.edumanage.fees.dto.BulkFeeAssignmentResultDto;
import com.edumanage.fees.dto.CreateFeeAssignmentRequest;
import com.edumanage.fees.dto.FeeAssignmentDto;
import com.edumanage.fees.service.FeeAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/fee-assignments")
@PreAuthorize("hasAnyAuthority('ROLE_ACCOUNTANT', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class FeeAssignmentController {

    private final FeeAssignmentService feeAssignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeeAssignmentDto>>> list() {
        var assignments = feeAssignmentService.list(org.springframework.data.domain.Pageable.unpaged()).getContent();
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee assignments fetched", assignments, null, LocalDateTime.now(), "/api/school/fee-assignments"));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<FeeAssignmentDto>>> listByStudent(@PathVariable Long studentId) {
        var assignments = feeAssignmentService.listByStudent(studentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee assignments fetched", assignments, null, LocalDateTime.now(), "/api/school/fee-assignments/student/" + studentId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeeAssignmentDto>> assign(@Valid @RequestBody CreateFeeAssignmentRequest request) {
        var assignment = feeAssignmentService.assign(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee assigned", assignment, null, LocalDateTime.now(), "/api/school/fee-assignments"));
    }

    @PostMapping("/bulk/preview")
    public ResponseEntity<ApiResponse<BulkFeeAssignmentPreviewDto>> preview(@Valid @RequestBody BulkFeeAssignmentRequest request) {
        var preview = feeAssignmentService.previewBulkAssignment(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bulk assignment preview fetched", preview, null, LocalDateTime.now(), "/api/school/fee-assignments/bulk/preview"));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkFeeAssignmentResultDto>> bulkAssign(@Valid @RequestBody BulkFeeAssignmentRequest request) {
        var result = feeAssignmentService.bulkAssign(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bulk assignment completed", result, null, LocalDateTime.now(), "/api/school/fee-assignments/bulk"));
    }
}
