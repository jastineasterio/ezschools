package com.edumanage.fees.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    private Long feeAssignmentId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    private String referenceNumber;

    private String receiptNumber;

    private String remarks;
}
