package com.edumanage.dashboards.dto;

import com.edumanage.transport.dto.RouteDto;
import com.edumanage.transport.dto.TransportAssignmentDto;
import com.edumanage.transport.dto.VehicleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Landing payload for the transport officer dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportOfficerDashboardDto {

    private Long schoolId;
    private String schoolName;
    private long totalVehicles;
    private long activeVehicles;
    private long totalRoutes;
    private long activeRoutes;
    private long activeAssignments;
    private long studentsOnTransport;
    private long vehiclesDueForService;
    private long vehiclesWithExpiringInsurance;
    private List<VehicleDto> vehicles = new ArrayList<>();
    private List<RouteDto> routes = new ArrayList<>();
    private List<TransportAssignmentDto> assignments = new ArrayList<>();
    private LocalDate serviceHorizon;
}
