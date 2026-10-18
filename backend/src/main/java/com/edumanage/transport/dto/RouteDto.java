package com.edumanage.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private String code;
    private String startPoint;
    private String endPoint;
    private java.math.BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private LocalTime morningDeparture;
    private LocalTime afternoonDeparture;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
