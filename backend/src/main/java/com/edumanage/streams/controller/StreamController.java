package com.edumanage.streams.controller;

import com.edumanage.streams.dto.CreateStreamRequest;
import com.edumanage.streams.dto.StreamDto;
import com.edumanage.streams.service.StreamService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/streams")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StreamDto>>> list(
            Pageable pageable, @RequestParam(required = false) String search) {
        var streams = streamService.list(pageable, search);
        return ResponseEntity.ok(new ApiResponse<>(true, "Streams fetched", streams, null, LocalDateTime.now(), "/api/school/streams"));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<StreamDto>>> listByClass(@PathVariable Long classId) {
        var streams = streamService.listByClass(classId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Streams fetched by class", streams, null, LocalDateTime.now(), "/api/school/streams/class/" + classId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StreamDto>> getById(@PathVariable Long id) {
        var stream = streamService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stream fetched", stream, null, LocalDateTime.now(), "/api/school/streams/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StreamDto>> create(@Valid @RequestBody CreateStreamRequest request) {
        var stream = streamService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stream created", stream, null, LocalDateTime.now(), "/api/school/streams"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StreamDto>> update(@PathVariable Long id,
                                                         @Valid @RequestBody CreateStreamRequest request) {
        var stream = streamService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stream updated", stream, null, LocalDateTime.now(), "/api/school/streams/" + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<StreamDto>> updateStatus(@PathVariable Long id,
                                                               @RequestParam String status) {
        var stream = streamService.updateStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stream status updated", stream, null, LocalDateTime.now(), "/api/school/streams/" + id + "/status"));
    }
}