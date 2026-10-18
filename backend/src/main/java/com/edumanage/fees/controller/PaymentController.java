package com.edumanage.fees.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.fees.dto.CreatePaymentRequest;
import com.edumanage.fees.dto.PaymentDto;
import com.edumanage.fees.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/payments")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentDto>>> list(Pageable pageable) {
        var payments = paymentService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payments fetched", payments, null, LocalDateTime.now(), "/api/school/payments"));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<java.util.List<PaymentDto>>> listByStudent(@PathVariable Long studentId) {
        var payments = paymentService.listByStudent(studentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payments fetched", payments, null, LocalDateTime.now(), "/api/school/payments/student/" + studentId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDto>> create(@Valid @RequestBody CreatePaymentRequest request) {
        var payment = paymentService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment recorded", payment, null, LocalDateTime.now(), "/api/school/payments"));
    }
}
