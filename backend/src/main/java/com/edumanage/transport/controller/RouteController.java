package com.edumanage.transport.controller;

import com.edumanage.transport.dto.CreateRouteRequest;
import com.edumanage.transport.dto.RouteDto;
import com.edumanage.transport.service.RouteService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/transport/routes")
@PreAuthorize("hasAnyAuthority('ROLE_TRANSPORT_OFFICER', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RouteDto>>> list(Pageable pageable) {
        var routes = routeService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Routes fetched", routes, null, LocalDateTime.now(), "/api/school/transport/routes"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteDto>> getById(@PathVariable Long id) {
        var route = routeService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route fetched", route, null, LocalDateTime.now(), "/api/school/transport/routes/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RouteDto>> create(@Valid @RequestBody CreateRouteRequest request) {
        var route = routeService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route created", route, null, LocalDateTime.now(), "/api/school/transport/routes"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteDto>> update(@PathVariable Long id,
                                                        @Valid @RequestBody CreateRouteRequest request) {
        var route = routeService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route updated", route, null, LocalDateTime.now(), "/api/school/transport/routes/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        routeService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route deleted", null, null, LocalDateTime.now(), "/api/school/transport/routes/" + id));
    }
}
