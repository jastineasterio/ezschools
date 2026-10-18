package com.edumanage.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRouteRequest {

    @NotBlank(message = "Route name is required")
    private String name;

    private String code;

    @NotBlank(message = "Start point is required")
    private String startPoint;

    @NotBlank(message = "End point is required")
    private String endPoint;

    private java.math.BigDecimal distanceKm;

    private Integer estimatedDurationMinutes;

    private LocalTime morningDeparture;

    private LocalTime afternoonDeparture;

    private String status;

    private String remarks;
}
