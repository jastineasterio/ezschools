package com.edumanage.boarding.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.boarding.dto.CreateBedRequest;
import com.edumanage.boarding.dto.BedDto;
import com.edumanage.boarding.service.BedService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/boarding/beds")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class BedController {

    private final BedService bedService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BedDto>>> listBeds(Pageable pageable) {
        var result = bedService.listBeds(pageable).getContent();
        return ResponseEntity.ok(new ApiResponse<>(true, "Beds fetched", result, null, LocalDateTime.now(), "/api/school/boarding/beds"));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<BedDto>>> listBedsByRoom(@PathVariable Long roomId) {
        var result = bedService.listBedsByRoom(roomId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Beds fetched", result, null, LocalDateTime.now(), "/api/school/boarding/beds/room/" + roomId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BedDto>> getBed(@PathVariable Long id) {
        var bed = bedService.getBed(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bed fetched", bed, null, LocalDateTime.now(), "/api/school/boarding/beds/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BedDto>> createBed(@Valid @RequestBody CreateBedRequest request) {
        var bed = bedService.createBed(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bed created", bed, null, LocalDateTime.now(), "/api/school/boarding/beds"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BedDto>> updateBed(@PathVariable Long id,
                                                         @Valid @RequestBody CreateBedRequest request) {
        var bed = bedService.updateBed(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bed updated", bed, null, LocalDateTime.now(), "/api/school/boarding/beds/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBed(@PathVariable Long id) {
        bedService.deleteBed(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bed deleted", null, null, LocalDateTime.now(), "/api/school/boarding/beds/" + id));
    }
}
