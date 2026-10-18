package com.edumanage.transport.dto;

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
public class CreateTransportAssignmentRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Route ID is required")
    private Long routeId;

    @NotNull(message = "Stop ID is required")
    private Long stopId;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    private String pickupPoint;

    private String dropPoint;

    private Long academicYearId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean feeApplicable;

    private BigDecimal monthlyFee;

    private String status;

    private String remarks;
}
