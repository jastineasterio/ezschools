package com.edumanage.transport.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.transport.dto.CreateRouteRequest;
import com.edumanage.transport.dto.RouteDto;
import com.edumanage.transport.entity.Route;
import com.edumanage.transport.repository.RouteRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final AuditLogService auditLogService;

    public RouteService(RouteRepository routeRepository, AuditLogService auditLogService) {
        this.routeRepository = routeRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<RouteDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Route> page;
        if (tenantId != null) {
            page = routeRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = routeRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public RouteDto getById(Long id) {
        var route = routeRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(route.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(route);
    }

    @Transactional
    public RouteDto create(CreateRouteRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (routeRepository.findBySchoolIdAndName(tenantId, request.getName()).isPresent()) {
            throw new BadRequestException("Route with this name already exists");
        }

        Route route = new Route();
        route.setSchoolId(tenantId);
        route.setName(request.getName());
        route.setCode(request.getCode());
        route.setStartPoint(request.getStartPoint());
        route.setEndPoint(request.getEndPoint());
        route.setDistanceKm(request.getDistanceKm());
        route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        route.setMorningDeparture(request.getMorningDeparture());
        route.setAfternoonDeparture(request.getAfternoonDeparture());
        route.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        route.setRemarks(request.getRemarks());

        routeRepository.save(route);

        auditLogService.log("ROUTE_CREATE", "Route", route.getId(),
                "Created route " + route.getName(), null, null, null);

        return mapToDto(route);
    }

    @Transactional
    public RouteDto update(Long id, CreateRouteRequest request) {
        var route = routeRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(route.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (routeRepository.findBySchoolIdAndName(tenantId, request.getName())
                .filter(r -> !r.getId().equals(id)).isPresent()) {
            throw new BadRequestException("Route with this name already exists");
        }

        route.setName(request.getName());
        route.setCode(request.getCode());
        route.setStartPoint(request.getStartPoint());
        route.setEndPoint(request.getEndPoint());
        route.setDistanceKm(request.getDistanceKm());
        route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        route.setMorningDeparture(request.getMorningDeparture());
        route.setAfternoonDeparture(request.getAfternoonDeparture());
        if (request.getStatus() != null) {
            route.setStatus(request.getStatus());
        }
        route.setRemarks(request.getRemarks());

        routeRepository.save(route);

        auditLogService.log("ROUTE_UPDATE", "Route", route.getId(),
                "Updated route " + route.getName(), null, null, null);

        return mapToDto(route);
    }

    @Transactional
    public void delete(Long id) {
        var route = routeRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(route.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        route.setDeletedAt(java.time.LocalDateTime.now());
        routeRepository.save(route);

        auditLogService.log("ROUTE_DELETE", "Route", route.getId(),
                "Deleted route " + route.getName(), null, null, null);
    }

    private RouteDto mapToDto(Route entity) {
        RouteDto dto = new RouteDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setStartPoint(entity.getStartPoint());
        dto.setEndPoint(entity.getEndPoint());
        dto.setDistanceKm(entity.getDistanceKm());
        dto.setEstimatedDurationMinutes(entity.getEstimatedDurationMinutes());
        dto.setMorningDeparture(entity.getMorningDeparture());
        dto.setAfternoonDeparture(entity.getAfternoonDeparture());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
