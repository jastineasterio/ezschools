package com.edumanage.fees.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeeAssignmentRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Fee structure ID is required")
    private Long feeStructureId;

    private LocalDate dueDate;
}
