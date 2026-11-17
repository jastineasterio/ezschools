package com.edumanage.sms.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.sms.dto.CreateSmsPurchaseRequest;
import com.edumanage.sms.dto.RetryPurchaseRequest;
import com.edumanage.sms.dto.SmsPurchaseDto;
import com.edumanage.sms.service.SmsPurchaseService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/sms/purchases")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
@Validated
public class SmsPurchaseController {

    private final SmsPurchaseService smsPurchaseService;

    @PostMapping
    public ResponseEntity<ApiResponse<SmsPurchaseDto>> createPurchase(
            @RequestParam Long schoolId,
            @RequestBody @Valid CreateSmsPurchaseRequest request) {
        var dto = smsPurchaseService.createPurchase(schoolId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS purchase initiated", dto, null,
                LocalDateTime.now(), "/api/platform/sms/purchases"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SmsPurchaseDto>>> getPurchases(
            @RequestParam Long schoolId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        var purchases = smsPurchaseService.getPurchases(schoolId, status, PageRequest.of(page, limit));
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS purchase history fetched",
                PageResponse.from(purchases), null, LocalDateTime.now(), "/api/platform/sms/purchases"));
    }

    @PostMapping("/{purchaseId}/pay")
    public ResponseEntity<ApiResponse<SmsPurchaseDto>> retryPurchase(
            @RequestParam Long schoolId,
            @PathVariable String purchaseId,
            @RequestBody @Valid RetryPurchaseRequest request) {
        var dto = smsPurchaseService.retryPurchase(schoolId, purchaseId, request.getPhone());
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment push triggered", dto, null,
                LocalDateTime.now(), "/api/platform/sms/purchases/" + purchaseId + "/pay"));
    }
}
