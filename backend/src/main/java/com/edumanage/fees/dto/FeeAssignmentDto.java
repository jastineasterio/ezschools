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
public class FeeAssignmentDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long feeStructureId;
    private String feeStructureName;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private BigDecimal balance;
    private LocalDate dueDate;
    private String status;
    private LocalDate assignedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
