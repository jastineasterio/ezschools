package com.edumanage.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFinancialTransactionRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Transaction type is required")
    private String transactionType;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    private String referenceNumber;

    private String description;

    private Long paymentId;

    private Long feeAssignmentId;
}
