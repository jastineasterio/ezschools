package com.edumanage.users.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.users.dto.CreateManagerRequest;
import com.edumanage.users.dto.UserDto;
import com.edumanage.users.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/users")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> listUsers(Pageable pageable) {
        var users = userService.listAllUsers(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", users, null, LocalDateTime.now(), "/api/platform/users"));
    }

    @PostMapping("/managers")
    public ResponseEntity<ApiResponse<UserDto>> createManager(@Valid @RequestBody CreateManagerRequest request,
                                                              HttpServletRequest httpRequest) {
        var user = userService.createManager(request, httpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "Manager created", user, null, LocalDateTime.now(), "/api/platform/users/managers"));
    }
}
