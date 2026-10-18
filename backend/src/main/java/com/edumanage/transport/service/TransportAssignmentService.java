package com.edumanage.transport.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.transport.dto.CreateTransportAssignmentRequest;
import com.edumanage.transport.dto.TransportAssignmentDto;
import com.edumanage.transport.entity.TransportAssignment;
import com.edumanage.transport.entity.Vehicle;
import com.edumanage.transport.repository.TransportAssignmentRepository;
import com.edumanage.transport.repository.VehicleRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransportAssignmentService {

    private final TransportAssignmentRepository transportAssignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditLogService auditLogService;

    public TransportAssignmentService(TransportAssignmentRepository transportAssignmentRepository,
                                      VehicleRepository vehicleRepository,
                                      StudentProfileRepository studentProfileRepository,
                                      AuditLogService auditLogService) {
        this.transportAssignmentRepository = transportAssignmentRepository;
        this.vehicleRepository = vehicleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<TransportAssignmentDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<TransportAssignment> page;
        if (tenantId != null) {
            page = transportAssignmentRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = transportAssignmentRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public PageResponse<TransportAssignmentDto> listByStudent(Long studentId, Pageable pageable) {
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Page<TransportAssignment> page = transportAssignmentRepository.findBySchoolIdAndStudentId(
                tenantId != null ? tenantId : student.getSchoolId(), studentId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public TransportAssignmentDto getById(Long id) {
        var assignment = transportAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Transport assignment not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(assignment.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(assignment);
    }

    @Transactional
    public TransportAssignmentDto create(CreateTransportAssignmentRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var vehicle = vehicleRepository.findById(request.getVehicleId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (!tenantId.equals(vehicle.getSchoolId())) {
            throw new ForbiddenException("Vehicle does not belong to this school");
        }

        if (transportAssignmentRepository.existsBySchoolIdAndStudentIdAndRouteIdAndStatus(
                tenantId, request.getStudentId(), request.getRouteId(), "ACTIVE")) {
            throw new ConflictException("Student is already assigned to an active route");
        }

        long activeAssignments = transportAssignmentRepository.countBySchoolIdAndVehicleIdAndStatus(
                tenantId, request.getVehicleId(), "ACTIVE");
        if (activeAssignments >= vehicle.getCapacity()) {
            throw new BadRequestException("Vehicle capacity exceeded. Current: " + activeAssignments + ", Capacity: " + vehicle.getCapacity());
        }

        TransportAssignment assignment = new TransportAssignment();
        assignment.setSchoolId(tenantId);
        assignment.setStudentId(request.getStudentId());
        assignment.setRouteId(request.getRouteId());
        assignment.setStopId(request.getStopId());
        assignment.setVehicleId(request.getVehicleId());
        assignment.setPickupPoint(request.getPickupPoint());
        assignment.setDropPoint(request.getDropPoint());
        assignment.setAcademicYearId(request.getAcademicYearId());
        assignment.setStartDate(request.getStartDate());
        assignment.setEndDate(request.getEndDate());
        assignment.setFeeApplicable(request.getFeeApplicable() != null ? request.getFeeApplicable() : false);
        assignment.setMonthlyFee(request.getMonthlyFee());
        assignment.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        assignment.setRemarks(request.getRemarks());

        transportAssignmentRepository.save(assignment);

        auditLogService.log("TRANSPORT_ASSIGNMENT_CREATE", "TransportAssignment", assignment.getId(),
                "Assigned student " + student.getFirstName() + " " + student.getLastName() + " to vehicle " + vehicle.getRegistrationNumber(),
                null, null, null);

        return mapToDto(assignment);
    }

    @Transactional
    public TransportAssignmentDto update(Long id, CreateTransportAssignmentRequest request) {
        var assignment = transportAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Transport assignment not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(assignment.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var vehicle = vehicleRepository.findById(request.getVehicleId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (!tenantId.equals(vehicle.getSchoolId())) {
            throw new ForbiddenException("Vehicle does not belong to this school");
        }

        long currentVehicleCount = transportAssignmentRepository.countBySchoolIdAndVehicleIdAndStatus(
                tenantId, request.getVehicleId(), "ACTIVE");
        if (currentVehicleCount >= vehicle.getCapacity() && !request.getVehicleId().equals(assignment.getVehicleId())) {
            throw new BadRequestException("Vehicle capacity exceeded. Current: " + currentVehicleCount + ", Capacity: " + vehicle.getCapacity());
        }

        assignment.setStudentId(request.getStudentId());
        assignment.setRouteId(request.getRouteId());
        assignment.setStopId(request.getStopId());
        assignment.setVehicleId(request.getVehicleId());
        assignment.setPickupPoint(request.getPickupPoint());
        assignment.setDropPoint(request.getDropPoint());
        assignment.setAcademicYearId(request.getAcademicYearId());
        assignment.setStartDate(request.getStartDate());
        assignment.setEndDate(request.getEndDate());
        assignment.setFeeApplicable(request.getFeeApplicable() != null ? request.getFeeApplicable() : false);
        assignment.setMonthlyFee(request.getMonthlyFee());
        if (request.getStatus() != null) {
            assignment.setStatus(request.getStatus());
        }
        assignment.setRemarks(request.getRemarks());

        transportAssignmentRepository.save(assignment);

        auditLogService.log("TRANSPORT_ASSIGNMENT_UPDATE", "TransportAssignment", assignment.getId(),
                "Updated transport assignment for student " + student.getFirstName() + " " + student.getLastName(),
                null, null, null);

        return mapToDto(assignment);
    }

    @Transactional
    public void delete(Long id) {
        var assignment = transportAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Transport assignment not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(assignment.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        assignment.setDeletedAt(java.time.LocalDateTime.now());
        transportAssignmentRepository.save(assignment);

        auditLogService.log("TRANSPORT_ASSIGNMENT_DELETE", "TransportAssignment", assignment.getId(),
                "Deleted transport assignment for student " + assignment.getStudentId(), null, null, null);
    }

    private TransportAssignmentDto mapToDto(TransportAssignment entity) {
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
        return dto;
    }
}
