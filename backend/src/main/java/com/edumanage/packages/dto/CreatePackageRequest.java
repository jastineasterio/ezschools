package com.edumanage.packages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePackageRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String description;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    @NotBlank(message = "Billing period is required")
    private String billingPeriod;

    @JsonProperty("active")
    private boolean isActive = true;

    @JsonProperty("default")
    private boolean isDefault = false;
    private int sortOrder = 0;
}
