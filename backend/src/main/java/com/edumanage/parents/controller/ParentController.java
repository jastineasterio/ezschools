package com.edumanage.parents.controller;

import com.edumanage.parents.dto.CreateParentRequest;
import com.edumanage.parents.dto.ParentDto;
import com.edumanage.parents.service.ParentService;
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
@RequestMapping("/school/parents")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ParentDto>>> list(Pageable pageable) {
        var parents = parentService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Parents fetched", parents, null, LocalDateTime.now(), "/api/school/parents"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ParentDto>> getById(@PathVariable Long id) {
        var parent = parentService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Parent fetched", parent, null, LocalDateTime.now(), "/api/school/parents/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ParentDto>> create(@Valid @RequestBody CreateParentRequest request) {
        var parent = parentService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Parent created", parent, null, LocalDateTime.now(), "/api/school/parents"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ParentDto>> update(@PathVariable Long id,
                                                         @Valid @RequestBody CreateParentRequest request) {
        var parent = parentService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Parent updated", parent, null, LocalDateTime.now(), "/api/school/parents/" + id));
    }
}