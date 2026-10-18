package com.edumanage.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportAssignmentDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long studentId;
    private String studentName;
    private Long routeId;
    private String routeName;
    private Long stopId;
    private String stopName;
    private Long vehicleId;
    private String vehicleRegistration;
    private String pickupPoint;
    private String dropPoint;
    private Long academicYearId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean feeApplicable;
    private BigDecimal monthlyFee;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
