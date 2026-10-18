package com.edumanage.transport.controller;

import com.edumanage.transport.dto.CreateRouteStopRequest;
import com.edumanage.transport.dto.RouteStopDto;
import com.edumanage.transport.service.RouteStopService;
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
@RequestMapping("/school/transport/routes/{routeId}/stops")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class RouteStopController {

    private final RouteStopService routeStopService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RouteStopDto>>> list(@PathVariable Long routeId, Pageable pageable) {
        var stops = routeStopService.listByRoute(routeId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route stops fetched", stops, null, LocalDateTime.now(), "/api/school/transport/routes/" + routeId + "/stops"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteStopDto>> getById(@PathVariable Long routeId, @PathVariable Long id) {
        var stop = routeStopService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route stop fetched", stop, null, LocalDateTime.now(), "/api/school/transport/routes/" + routeId + "/stops/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RouteStopDto>> create(@PathVariable Long routeId,
                                                            @Valid @RequestBody CreateRouteStopRequest request) {
        var stop = routeStopService.create(routeId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route stop created", stop, null, LocalDateTime.now(), "/api/school/transport/routes/" + routeId + "/stops"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteStopDto>> update(@PathVariable Long routeId, @PathVariable Long id,
                                                            @Valid @RequestBody CreateRouteStopRequest request) {
        var stop = routeStopService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route stop updated", stop, null, LocalDateTime.now(), "/api/school/transport/routes/" + routeId + "/stops/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long routeId, @PathVariable Long id) {
        routeStopService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route stop deleted", null, null, LocalDateTime.now(), "/api/school/transport/routes/" + routeId + "/stops/" + id));
    }
}
