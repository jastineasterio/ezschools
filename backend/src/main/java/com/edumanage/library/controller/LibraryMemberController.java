package com.edumanage.library.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.library.dto.CreateLibraryMemberRequest;
import com.edumanage.library.dto.LibraryMemberDto;
import com.edumanage.library.service.LibraryMemberService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/library/members")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class LibraryMemberController {

    private final LibraryMemberService libraryMemberService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LibraryMemberDto>>> list(Pageable pageable) {
        var members = libraryMemberService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Library members fetched", members, null, LocalDateTime.now(), "/api/school/library/members"));
    }

    @GetMapping("/type/{memberType}")
    public ResponseEntity<ApiResponse<java.util.List<LibraryMemberDto>>> listByType(@PathVariable String memberType) {
        var members = libraryMemberService.listByMemberType(memberType);
        return ResponseEntity.ok(new ApiResponse<>(true, "Library members fetched", members, null, LocalDateTime.now(), "/api/school/library/members/type/" + memberType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LibraryMemberDto>> get(@PathVariable Long id) {
        var member = libraryMemberService.get(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Library member fetched", member, null, LocalDateTime.now(), "/api/school/library/members/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LibraryMemberDto>> create(@Valid @RequestBody CreateLibraryMemberRequest request) {
        var member = libraryMemberService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Library member created", member, null, LocalDateTime.now(), "/api/school/library/members"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LibraryMemberDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody CreateLibraryMemberRequest request) {
        var member = libraryMemberService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Library member updated", member, null, LocalDateTime.now(), "/api/school/library/members/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        libraryMemberService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Library member deleted", null, null, LocalDateTime.now(), "/api/school/library/members/" + id));
    }
}
