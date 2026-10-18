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
public class FinancialCategoryDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private String categoryType;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
