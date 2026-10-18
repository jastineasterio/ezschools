package com.edumanage.transport.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.transport.dto.CreateRouteStopRequest;
import com.edumanage.transport.dto.RouteStopDto;
import com.edumanage.transport.entity.Route;
import com.edumanage.transport.entity.RouteStop;
import com.edumanage.transport.repository.RouteRepository;
import com.edumanage.transport.repository.RouteStopRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteStopService {

    private final RouteStopRepository routeStopRepository;
    private final RouteRepository routeRepository;
    private final AuditLogService auditLogService;

    public RouteStopService(RouteStopRepository routeStopRepository, RouteRepository routeRepository, AuditLogService auditLogService) {
        this.routeStopRepository = routeStopRepository;
        this.routeRepository = routeRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<RouteStopDto> listByRoute(Long routeId, Pageable pageable) {
        var route = routeRepository.findById(routeId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(route.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Page<RouteStop> page = routeStopRepository.findByRouteId(routeId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public RouteStopDto getById(Long id) {
        var stop = routeStopRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Route stop not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(stop.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(stop);
    }

    @Transactional
    public RouteStopDto create(Long routeId, CreateRouteStopRequest request) {
        var route = routeRepository.findById(routeId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || !tenantId.equals(route.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (request.getStopOrder() == null || request.getStopOrder() <= 0) {
            throw new BadRequestException("Stop order must be a positive number");
        }

        RouteStop stop = new RouteStop();
        stop.setRouteId(routeId);
        stop.setSchoolId(tenantId);
        stop.setName(request.getName());
        stop.setStopOrder(request.getStopOrder());
        stop.setLatitude(request.getLatitude());
        stop.setLongitude(request.getLongitude());
        stop.setArrivalTime(request.getArrivalTime());
        stop.setDepartureTime(request.getDepartureTime());
        stop.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        stop.setRemarks(request.getRemarks());

        routeStopRepository.save(stop);

        auditLogService.log("ROUTE_STOP_CREATE", "RouteStop", stop.getId(),
                "Created stop " + stop.getName() + " on route " + route.getName(), null, null, null);

        return mapToDto(stop);
    }

    @Transactional
    public RouteStopDto update(Long id, CreateRouteStopRequest request) {
        var stop = routeStopRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Route stop not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(stop.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (request.getStopOrder() == null || request.getStopOrder() <= 0) {
            throw new BadRequestException("Stop order must be a positive number");
        }

        stop.setName(request.getName());
        stop.setStopOrder(request.getStopOrder());
        stop.setLatitude(request.getLatitude());
        stop.setLongitude(request.getLongitude());
        stop.setArrivalTime(request.getArrivalTime());
        stop.setDepartureTime(request.getDepartureTime());
        if (request.getStatus() != null) {
            stop.setStatus(request.getStatus());
        }
        stop.setRemarks(request.getRemarks());

        routeStopRepository.save(stop);

        auditLogService.log("ROUTE_STOP_UPDATE", "RouteStop", stop.getId(),
                "Updated stop " + stop.getName(), null, null, null);

        return mapToDto(stop);
    }

    @Transactional
    public void delete(Long id) {
        var stop = routeStopRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Route stop not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(stop.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        stop.setDeletedAt(java.time.LocalDateTime.now());
        routeStopRepository.save(stop);

        auditLogService.log("ROUTE_STOP_DELETE", "RouteStop", stop.getId(),
                "Deleted stop " + stop.getName(), null, null, null);
    }

    private RouteStopDto mapToDto(RouteStop entity) {
        RouteStopDto dto = new RouteStopDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setRouteId(entity.getRouteId());
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setStopOrder(entity.getStopOrder());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setArrivalTime(entity.getArrivalTime());
        dto.setDepartureTime(entity.getDepartureTime());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
