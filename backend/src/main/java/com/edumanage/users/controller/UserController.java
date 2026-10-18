package com.edumanage.users.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.users.dto.CreateUserRequest;
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
@RequestMapping("/school/users")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> listUsers(Pageable pageable) {
        var users = userService.listUsers(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", users, null, LocalDateTime.now(), "/api/school/users"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        var user = userService.getUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User fetched", user, null, LocalDateTime.now(), "/api/school/users/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request,
                                                           HttpServletRequest httpRequest) {
        var user = userService.createUser(request, httpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "User created", user, null, LocalDateTime.now(), "/api/school/users"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long id,
                                                           @Valid @RequestBody CreateUserRequest request,
                                                           HttpServletRequest httpRequest) {
        var user = userService.updateUser(id, request, httpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated", user, null, LocalDateTime.now(), "/api/school/users/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id,
                                                        HttpServletRequest httpRequest) {
        userService.deleteUser(id, httpRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "User deleted", null, null, LocalDateTime.now(), "/api/school/users/" + id));
    }
}
