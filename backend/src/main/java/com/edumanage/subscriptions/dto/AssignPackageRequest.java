package com.edumanage.subscriptions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignPackageRequest {
    @NotNull(message = "Package ID is required")
    private Long packageId;

    private String billingPeriod = "MONTHLY";
}
