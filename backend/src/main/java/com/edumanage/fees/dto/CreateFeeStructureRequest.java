package com.edumanage.fees.dto;

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
public class CreateFeeStructureRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private LocalDate dueDate;

    private Long academicYearId;

    private Long classId;

    @NotBlank(message = "Fee type is required")
    private String feeType;

    private Boolean isMandatory;

    private String status;
}
