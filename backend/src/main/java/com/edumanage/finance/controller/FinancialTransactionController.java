package com.edumanage.finance.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.finance.dto.CreateFinancialTransactionRequest;
import com.edumanage.finance.dto.FinancialTransactionDto;
import com.edumanage.finance.service.FinancialTransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/finance/transactions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class FinancialTransactionController {

    private final FinancialTransactionService financialTransactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FinancialTransactionDto>>> list(Pageable pageable) {
        var transactions = financialTransactionService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transactions fetched", transactions, null, LocalDateTime.now(), "/api/school/finance/transactions"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> create(@Valid @RequestBody CreateFinancialTransactionRequest request) {
        var transaction = financialTransactionService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transaction created", transaction, null, LocalDateTime.now(), "/api/school/finance/transactions"));
    }
}
