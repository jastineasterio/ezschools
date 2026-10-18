package com.edumanage.transport.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.transport.dto.CreateVehicleRequest;
import com.edumanage.transport.dto.VehicleDto;
import com.edumanage.transport.entity.Vehicle;
import com.edumanage.transport.repository.VehicleRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final AuditLogService auditLogService;

    public VehicleService(VehicleRepository vehicleRepository, AuditLogService auditLogService) {
        this.vehicleRepository = vehicleRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<VehicleDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Vehicle> page;
        if (tenantId != null) {
            page = vehicleRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = vehicleRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public VehicleDto getById(Long id) {
        var vehicle = vehicleRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(vehicle.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(vehicle);
    }

    @Transactional
    public VehicleDto create(CreateVehicleRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (vehicleRepository.findBySchoolIdAndRegistrationNumber(tenantId, request.getRegistrationNumber()).isPresent()) {
            throw new BadRequestException("Vehicle with this registration number already exists");
        }

        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new BadRequestException("Capacity must be a positive number");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setSchoolId(tenantId);
        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setVehicleType(request.getVehicleType() != null ? request.getVehicleType() : "BUS");
        vehicle.setCapacity(request.getCapacity());
        vehicle.setDriverId(request.getDriverId());
        vehicle.setDriverName(request.getDriverName());
        vehicle.setDriverPhone(request.getDriverPhone());
        vehicle.setInsuranceNumber(request.getInsuranceNumber());
        vehicle.setInsuranceExpiry(request.getInsuranceExpiry());
        vehicle.setLastServiceDate(request.getLastServiceDate());
        vehicle.setNextServiceDate(request.getNextServiceDate());
        vehicle.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        vehicle.setRemarks(request.getRemarks());

        vehicleRepository.save(vehicle);

        auditLogService.log("VEHICLE_CREATE", "Vehicle", vehicle.getId(),
                "Created vehicle " + vehicle.getRegistrationNumber(), null, null, null);

        return mapToDto(vehicle);
    }

    @Transactional
    public VehicleDto update(Long id, CreateVehicleRequest request) {
        var vehicle = vehicleRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(vehicle.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (vehicleRepository.findBySchoolIdAndRegistrationNumber(tenantId, request.getRegistrationNumber())
                .filter(v -> !v.getId().equals(id)).isPresent()) {
            throw new BadRequestException("Vehicle with this registration number already exists");
        }

        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new BadRequestException("Capacity must be a positive number");
        }

        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setVehicleType(request.getVehicleType() != null ? request.getVehicleType() : "BUS");
        vehicle.setCapacity(request.getCapacity());
        vehicle.setDriverId(request.getDriverId());
        vehicle.setDriverName(request.getDriverName());
        vehicle.setDriverPhone(request.getDriverPhone());
        vehicle.setInsuranceNumber(request.getInsuranceNumber());
        vehicle.setInsuranceExpiry(request.getInsuranceExpiry());
        vehicle.setLastServiceDate(request.getLastServiceDate());
        vehicle.setNextServiceDate(request.getNextServiceDate());
        if (request.getStatus() != null) {
            vehicle.setStatus(request.getStatus());
        }
        vehicle.setRemarks(request.getRemarks());

        vehicleRepository.save(vehicle);

        auditLogService.log("VEHICLE_UPDATE", "Vehicle", vehicle.getId(),
                "Updated vehicle " + vehicle.getRegistrationNumber(), null, null, null);

        return mapToDto(vehicle);
    }

    @Transactional
    public void delete(Long id) {
        var vehicle = vehicleRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(vehicle.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        vehicle.setDeletedAt(java.time.LocalDateTime.now());
        vehicleRepository.save(vehicle);

        auditLogService.log("VEHICLE_DELETE", "Vehicle", vehicle.getId(),
                "Deleted vehicle " + vehicle.getRegistrationNumber(), null, null, null);
    }

    private VehicleDto mapToDto(Vehicle entity) {
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
}
