package com.edumanage.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String registrationNumber;
    private String vehicleType;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
