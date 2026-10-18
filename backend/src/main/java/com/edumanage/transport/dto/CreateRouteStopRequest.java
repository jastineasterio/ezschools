package com.edumanage.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRouteStopRequest {

    @NotNull(message = "Route ID is required")
    private Long routeId;

    @NotBlank(message = "Stop name is required")
    private String name;

    @NotNull(message = "Stop order is required")
    private Integer stopOrder;

    private java.math.BigDecimal latitude;

    private java.math.BigDecimal longitude;

    private LocalTime arrivalTime;

    private LocalTime departureTime;

    private String status;

    private String remarks;
}
