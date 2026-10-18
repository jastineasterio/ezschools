package com.edumanage.transport.controller;

import com.edumanage.transport.dto.CreateVehicleRequest;
import com.edumanage.transport.dto.VehicleDto;
import com.edumanage.transport.service.VehicleService;
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
@RequestMapping("/school/transport/vehicles")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VehicleDto>>> list(Pageable pageable) {
        var vehicles = vehicleService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicles fetched", vehicles, null, LocalDateTime.now(), "/api/school/transport/vehicles"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleDto>> getById(@PathVariable Long id) {
        var vehicle = vehicleService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle fetched", vehicle, null, LocalDateTime.now(), "/api/school/transport/vehicles/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleDto>> create(@Valid @RequestBody CreateVehicleRequest request) {
        var vehicle = vehicleService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle created", vehicle, null, LocalDateTime.now(), "/api/school/transport/vehicles"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleDto>> update(@PathVariable Long id,
                                                          @Valid @RequestBody CreateVehicleRequest request) {
        var vehicle = vehicleService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle updated", vehicle, null, LocalDateTime.now(), "/api/school/transport/vehicles/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle deleted", null, null, LocalDateTime.now(), "/api/school/transport/vehicles/" + id));
    }
}
