package com.edumanage.dashboards.service;

import com.edumanage.dashboards.dto.TransportOfficerDashboardDto;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.AccessGuard;
import com.edumanage.security.CurrentUserService;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.transport.dto.RouteDto;
import com.edumanage.transport.dto.TransportAssignmentDto;
import com.edumanage.transport.dto.VehicleDto;
import com.edumanage.transport.entity.Route;
import com.edumanage.transport.entity.RouteStop;
import com.edumanage.transport.entity.TransportAssignment;
import com.edumanage.transport.entity.Vehicle;
import com.edumanage.transport.repository.RouteRepository;
import com.edumanage.transport.repository.RouteStopRepository;
import com.edumanage.transport.repository.TransportAssignmentRepository;
import com.edumanage.transport.repository.VehicleRepository;
import com.edumanage.users.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the transport officer dashboard for the officer's own school.
 */
@Service
public class TransportOfficerDashboardService {

    private static final int MAX_ROWS = 500;
    private static final int SERVICE_HORIZON_DAYS = 30;

    private final AccessGuard accessGuard;
    private final CurrentUserService currentUserService;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final TransportAssignmentRepository transportAssignmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SchoolRepository schoolRepository;

    public TransportOfficerDashboardService(AccessGuard accessGuard,
                                            CurrentUserService currentUserService,
                                            VehicleRepository vehicleRepository,
                                            RouteRepository routeRepository,
                                            RouteStopRepository routeStopRepository,
                                            TransportAssignmentRepository transportAssignmentRepository,
                                            StudentProfileRepository studentProfileRepository,
                                            SchoolRepository schoolRepository) {
        this.accessGuard = accessGuard;
        this.currentUserService = currentUserService;
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.transportAssignmentRepository = transportAssignmentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.schoolRepository = schoolRepository;
    }

    @Transactional(readOnly = true)
    public TransportOfficerDashboardDto getDashboard() {
        accessGuard.assertAnyRole("TRANSPORT_OFFICER", "MANAGER", "SUPER_ADMIN");

        User user = currentUserService.requireCurrentUser();
        Long schoolId = user.getSchoolId();
        if (schoolId == null) {
            throw new ForbiddenException("A school assignment is required");
        }

        TransportOfficerDashboardDto dto = new TransportOfficerDashboardDto();
        dto.setSchoolId(schoolId);
        dto.setSchoolName(schoolRepository.findById(schoolId).map(school -> school.getName()).orElse(null));

        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(SERVICE_HORIZON_DAYS);
        dto.setServiceHorizon(horizon);

        List<Vehicle> vehicles = vehicleRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_ROWS)).getContent();
        dto.setTotalVehicles(vehicles.size());
        dto.setActiveVehicles(vehicles.stream()
                .filter(vehicle -> !vehicle.isDeleted())
                .filter(vehicle -> "ACTIVE".equalsIgnoreCase(vehicle.getStatus()))
                .count());
        dto.setVehiclesDueForService(vehicles.stream()
                .filter(vehicle -> !vehicle.isDeleted())
                .filter(vehicle -> vehicle.getNextServiceDate() != null && !vehicle.getNextServiceDate().isAfter(horizon))
                .count());
        dto.setVehiclesWithExpiringInsurance(vehicles.stream()
                .filter(vehicle -> !vehicle.isDeleted())
                .filter(vehicle -> vehicle.getInsuranceExpiry() != null && !vehicle.getInsuranceExpiry().isAfter(horizon))
                .count());
        dto.setVehicles(vehicles.stream()
                .filter(vehicle -> !vehicle.isDeleted())
                .map(this::mapVehicle)
                .toList());

        List<Route> routes = routeRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_ROWS)).getContent();
        dto.setTotalRoutes(routes.size());
        dto.setActiveRoutes(routes.stream()
                .filter(route -> !route.isDeleted())
                .filter(route -> "ACTIVE".equalsIgnoreCase(route.getStatus()))
                .count());
        dto.setRoutes(routes.stream()
                .filter(route -> !route.isDeleted())
                .map(this::mapRoute)
                .toList());

        List<TransportAssignment> assignments = transportAssignmentRepository.findBySchoolIdAndStatus(schoolId, "ACTIVE").stream()
                .filter(assignment -> !assignment.isDeleted())
                .toList();
        dto.setActiveAssignments(assignments.size());

        Set<Long> studentIds = new HashSet<>();
        assignments.forEach(assignment -> studentIds.add(assignment.getStudentId()));
        dto.setStudentsOnTransport(studentIds.size());

        dto.setAssignments(assignments.stream().map(this::mapAssignment).toList());
        return dto;
    }

    private VehicleDto mapVehicle(Vehicle entity) {
        VehicleDto dto = new VehicleDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setRegistrationNumber(entity.getRegistrationNumber());
        dto.setVehicleType(entity.getVehicleType());
        dto.setCapacity(entity.getCapacity());
        dto.setDriverId(entity.getDriverId());
        dto.setDriverName(entity.getDriverName());
        dto.setDriverPhone(entity.getDriverPhone());
        dto.setInsuranceNumber(entity.getInsuranceNumber());
        dto.setInsuranceExpiry(entity.getInsuranceExpiry());
        dto.setLastServiceDate(entity.getLastServiceDate());
        dto.setNextServiceDate(entity.getNextServiceDate());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private RouteDto mapRoute(Route entity) {
        RouteDto dto = new RouteDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setStartPoint(entity.getStartPoint());
        dto.setEndPoint(entity.getEndPoint());
        dto.setDistanceKm(entity.getDistanceKm());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private TransportAssignmentDto mapAssignment(TransportAssignment entity) {
        TransportAssignmentDto dto = new TransportAssignmentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());
        dto.setRouteId(entity.getRouteId());
        dto.setStopId(entity.getStopId());
        dto.setVehicleId(entity.getVehicleId());
        dto.setPickupPoint(entity.getPickupPoint());
        dto.setDropPoint(entity.getDropPoint());
        dto.setAcademicYearId(entity.getAcademicYearId());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setFeeApplicable(entity.getFeeApplicable());
        dto.setMonthlyFee(entity.getMonthlyFee());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        studentProfileRepository.findById(entity.getStudentId()).ifPresent(student ->
                dto.setStudentName(student.getFirstName() + " " + student.getLastName()));
        if (entity.getRouteId() != null) {
            routeRepository.findById(entity.getRouteId()).ifPresent(route -> dto.setRouteName(route.getName()));
        }
        if (entity.getStopId() != null) {
            routeStopRepository.findById(entity.getStopId()).ifPresent(stop -> dto.setStopName(stop.getName()));
        }
        if (entity.getVehicleId() != null) {
            vehicleRepository.findById(entity.getVehicleId()).ifPresent(vehicle -> dto.setVehicleRegistration(vehicle.getRegistrationNumber()));
        }
        return dto;
    }
}
