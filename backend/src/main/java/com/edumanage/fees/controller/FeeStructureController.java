package com.edumanage.fees.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.fees.dto.CreateFeeStructureRequest;
import com.edumanage.fees.dto.FeeStructureDto;
import com.edumanage.fees.service.FeeStructureService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/fee-structures")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeeStructureDto>>> list(Pageable pageable) {
        var fees = feeStructureService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee structures fetched", fees, null, LocalDateTime.now(), "/api/school/fee-structures"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeStructureDto>> getById(@PathVariable Long id) {
        var fee = feeStructureService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee structure fetched", fee, null, LocalDateTime.now(), "/api/school/fee-structures/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeeStructureDto>> create(@Valid @RequestBody CreateFeeStructureRequest request) {
        var fee = feeStructureService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee structure created", fee, null, LocalDateTime.now(), "/api/school/fee-structures"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeStructureDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody CreateFeeStructureRequest request) {
        var fee = feeStructureService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee structure updated", fee, null, LocalDateTime.now(), "/api/school/fee-structures/" + id));
    }
}
