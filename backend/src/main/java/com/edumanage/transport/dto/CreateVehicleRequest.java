package com.edumanage.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVehicleRequest {

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotNull(message = "Capacity is required")
    private Integer capacity;

    private Long driverId;

    private String driverName;

    private String driverPhone;

    private String insuranceNumber;

    private LocalDate insuranceExpiry;

    private LocalDate lastServiceDate;

    private LocalDate nextServiceDate;

    private String status;

    private String remarks;
}
