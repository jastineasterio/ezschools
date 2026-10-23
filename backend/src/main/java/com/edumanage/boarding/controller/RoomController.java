package com.edumanage.boarding.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.boarding.dto.CreateRoomRequest;
import com.edumanage.boarding.dto.RoomDto;
import com.edumanage.boarding.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/boarding/rooms")
@PreAuthorize("hasAnyAuthority('ROLE_WARDEN', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomDto>>> listRooms(Pageable pageable) {
        var result = roomService.listRooms(pageable).getContent();
        return ResponseEntity.ok(new ApiResponse<>(true, "Rooms fetched", result, null, LocalDateTime.now(), "/api/school/boarding/rooms"));
    }

    @GetMapping("/dormitory/{dormitoryId}")
    public ResponseEntity<ApiResponse<List<RoomDto>>> listRoomsByDormitory(@PathVariable Long dormitoryId) {
        var result = roomService.listRoomsByDormitory(dormitoryId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rooms fetched", result, null, LocalDateTime.now(), "/api/school/boarding/rooms/dormitory/" + dormitoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDto>> getRoom(@PathVariable Long id) {
        var room = roomService.getRoom(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Room fetched", room, null, LocalDateTime.now(), "/api/school/boarding/rooms/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        var room = roomService.createRoom(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Room created", room, null, LocalDateTime.now(), "/api/school/boarding/rooms"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDto>> updateRoom(@PathVariable Long id,
                                                           @Valid @RequestBody CreateRoomRequest request) {
        var room = roomService.updateRoom(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Room updated", room, null, LocalDateTime.now(), "/api/school/boarding/rooms/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Room deleted", null, null, LocalDateTime.now(), "/api/school/boarding/rooms/" + id));
    }
}
