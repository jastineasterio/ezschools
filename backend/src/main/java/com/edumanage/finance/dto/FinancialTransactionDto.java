package com.edumanage.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialTransactionDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String transactionType;
    private LocalDate transactionDate;
    private String referenceNumber;
    private String description;
    private Long paymentId;
    private Long feeAssignmentId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
