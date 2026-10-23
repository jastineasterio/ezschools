package com.edumanage.transport.controller;

import com.edumanage.transport.dto.CreateTransportAssignmentRequest;
import com.edumanage.transport.dto.TransportAssignmentDto;
import com.edumanage.transport.service.TransportAssignmentService;
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
@RequestMapping("/school/transport/assignments")
@PreAuthorize("hasAnyAuthority('ROLE_TRANSPORT_OFFICER', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class TransportAssignmentController {

    private final TransportAssignmentService transportAssignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransportAssignmentDto>>> list(Pageable pageable) {
        var assignments = transportAssignmentService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport assignments fetched", assignments, null, LocalDateTime.now(), "/api/school/transport/assignments"));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<PageResponse<TransportAssignmentDto>>> listByStudent(@PathVariable Long studentId, Pageable pageable) {
        var assignments = transportAssignmentService.listByStudent(studentId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport assignments fetched by student", assignments, null, LocalDateTime.now(), "/api/school/transport/assignments/student/" + studentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransportAssignmentDto>> getById(@PathVariable Long id) {
        var assignment = transportAssignmentService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport assignment fetched", assignment, null, LocalDateTime.now(), "/api/school/transport/assignments/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransportAssignmentDto>> create(@Valid @RequestBody CreateTransportAssignmentRequest request) {
        var assignment = transportAssignmentService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport assignment created", assignment, null, LocalDateTime.now(), "/api/school/transport/assignments"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransportAssignmentDto>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody CreateTransportAssignmentRequest request) {
        var assignment = transportAssignmentService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport assignment updated", assignment, null, LocalDateTime.now(), "/api/school/transport/assignments/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        transportAssignmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport assignment deleted", null, null, LocalDateTime.now(), "/api/school/transport/assignments/" + id));
    }
}
