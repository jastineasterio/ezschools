package com.edumanage.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFinancialCategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Category type is required")
    private String categoryType;

    private String description;

    private String status;
}
