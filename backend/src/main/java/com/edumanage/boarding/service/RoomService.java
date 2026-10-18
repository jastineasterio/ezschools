package com.edumanage.boarding.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.boarding.dto.CreateRoomRequest;
import com.edumanage.boarding.dto.RoomDto;
import com.edumanage.boarding.entity.Dormitory;
import com.edumanage.boarding.entity.Room;
import com.edumanage.boarding.repository.DormitoryRepository;
import com.edumanage.boarding.repository.RoomRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final DormitoryRepository dormitoryRepository;
    private final AuditLogService auditLogService;

    public RoomService(RoomRepository roomRepository, DormitoryRepository dormitoryRepository, AuditLogService auditLogService) {
        this.roomRepository = roomRepository;
        this.dormitoryRepository = dormitoryRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<RoomDto> listRooms(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Room> page;
        if (tenantId != null) {
            page = roomRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = roomRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<RoomDto> listRoomsByDormitory(Long dormitoryId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        var dormitory = dormitoryRepository.findBySchoolIdAndId(tenantId, dormitoryId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));

        return roomRepository.findBySchoolIdAndDormitoryId(tenantId, dormitoryId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public RoomDto getRoom(Long id) {
        var room = findByIdAndSchool(id);
        return mapToDto(room);
    }

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var dormitory = dormitoryRepository.findBySchoolIdAndId(tenantId, request.getDormitoryId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));

        if (roomRepository.existsBySchoolIdAndDormitoryIdAndRoomNumber(tenantId, request.getDormitoryId(), request.getRoomNumber())) {
            throw new ConflictException("Room number already exists in this dormitory");
        }

        Room room = new Room();
        room.setSchoolId(tenantId);
        room.setDormitoryId(request.getDormitoryId());
        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity() != null ? request.getCapacity() : 0);
        room.setCurrentOccupancy(0);
        room.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        roomRepository.save(room);

        auditLogService.log("ROOM_CREATE", "Room", room.getId(),
                "Created room " + room.getRoomNumber() + " in dormitory " + dormitory.getName(), null, null, null);

        return mapToDto(room);
    }

    @Transactional
    public RoomDto updateRoom(Long id, CreateRoomRequest request) {
        var room = findByIdAndSchool(id);

        if (!room.getDormitoryId().equals(request.getDormitoryId()) ||
                !room.getRoomNumber().equals(request.getRoomNumber())) {
            if (roomRepository.existsBySchoolIdAndDormitoryIdAndRoomNumber(room.getSchoolId(), request.getDormitoryId(), request.getRoomNumber())) {
                throw new ConflictException("Room number already exists in this dormitory");
            }
        }

        var dormitory = dormitoryRepository.findBySchoolIdAndId(room.getSchoolId(), request.getDormitoryId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));

        room.setDormitoryId(request.getDormitoryId());
        room.setRoomNumber(request.getRoomNumber());
        if (request.getCapacity() != null) {
            room.setCapacity(request.getCapacity());
        }
        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        }

        roomRepository.save(room);

        auditLogService.log("ROOM_UPDATE", "Room", room.getId(),
                "Updated room " + room.getRoomNumber() + " in dormitory " + dormitory.getName(), null, null, null);

        return mapToDto(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        var room = findByIdAndSchool(id);
        room.setDeletedAt(java.time.LocalDateTime.now());
        roomRepository.save(room);

        auditLogService.log("ROOM_DELETE", "Room", room.getId(),
                "Deleted room " + room.getRoomNumber(), null, null, null);
    }

    private Room findByIdAndSchool(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return roomRepository.findBySchoolIdAndId(tenantId, id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    private RoomDto mapToDto(Room room) {
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setUuid(room.getUuid() != null ? room.getUuid().toString() : null);
        dto.setSchoolId(room.getSchoolId());
        dto.setDormitoryId(room.getDormitoryId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setCapacity(room.getCapacity());
        dto.setCurrentOccupancy(room.getCurrentOccupancy());
        dto.setStatus(room.getStatus());
        dto.setCreatedAt(room.getCreatedAt());
        dto.setUpdatedAt(room.getUpdatedAt());
        return dto;
    }
}
