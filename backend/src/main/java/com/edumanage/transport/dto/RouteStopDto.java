package com.edumanage.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStopDto {
    private Long id;
    private String uuid;
    private Long routeId;
    private Long schoolId;
    private String name;
    private Integer stopOrder;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
