package com.edumanage.subscriptions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignPackageRequest {
    @NotNull(message = "Package ID is required")
    private Long packageId;

    private String billingPeriod;

    private LocalDate startDate;

    public AssignPackageRequest(Long packageId, String billingPeriod) {
        this(packageId, billingPeriod, null);
    }
}
