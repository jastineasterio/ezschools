package com.edumanage.fees.dto;

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
public class PaymentDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long feeAssignmentId;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDate paymentDate;
    private String referenceNumber;
    private String receiptNumber;
    private String remarks;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
