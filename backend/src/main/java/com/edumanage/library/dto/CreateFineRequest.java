package com.edumanage.library.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFineRequest {
    @NotNull(message = "Borrowing ID is required")
    private Long borrowingId;

    @NotNull(message = "Fine amount is required")
    @DecimalMin(value = "0.0", message = "Fine amount must be non-negative")
    private BigDecimal fineAmount;

    private String fineType = "OVERDUE";

    private String remarks;
}
