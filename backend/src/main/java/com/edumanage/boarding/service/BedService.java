package com.edumanage.boarding.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.boarding.dto.CreateBedRequest;
import com.edumanage.boarding.dto.BedDto;
import com.edumanage.boarding.entity.Bed;
import com.edumanage.boarding.entity.Room;
import com.edumanage.boarding.repository.BedRepository;
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
public class BedService {

    private final BedRepository bedRepository;
    private final RoomRepository roomRepository;
    private final AuditLogService auditLogService;

    public BedService(BedRepository bedRepository, RoomRepository roomRepository, AuditLogService auditLogService) {
        this.bedRepository = bedRepository;
        this.roomRepository = roomRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<BedDto> listBeds(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Bed> page;
        if (tenantId != null) {
            page = bedRepository.findBySchoolId(tenantId, pageable);
        } else if (TenantContext.isSuperAdmin()) {
            page = bedRepository.findAll(pageable);
        } else {
            throw new ForbiddenException("A school assignment is required");
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<BedDto> listBedsByRoom(Long roomId) {
        Long tenantId = TenantContext.requireTenantId();
        var room = roomRepository.findBySchoolIdAndId(tenantId, roomId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        return bedRepository.findBySchoolIdAndRoomId(tenantId, roomId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public BedDto getBed(Long id) {
        var bed = findByIdAndSchool(id);
        return mapToDto(bed);
    }

    @Transactional
    public BedDto createBed(CreateBedRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        var room = roomRepository.findBySchoolIdAndId(tenantId, request.getRoomId())
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (bedRepository.existsBySchoolIdAndRoomIdAndBedNumber(tenantId, request.getRoomId(), request.getBedNumber())) {
            throw new ConflictException("Bed number already exists in this room");
        }

        Bed bed = new Bed();
        bed.setSchoolId(tenantId);
        bed.setRoomId(request.getRoomId());
        bed.setBedNumber(request.getBedNumber());
        bed.setStatus(request.getStatus() != null ? request.getStatus() : "AVAILABLE");

        bedRepository.save(bed);

        auditLogService.log("BED_CREATE", "Bed", bed.getId(),
                "Created bed " + bed.getBedNumber() + " in room " + room.getRoomNumber(), null, null, null);

        return mapToDto(bed);
    }

    @Transactional
    public BedDto updateBed(Long id, CreateBedRequest request) {
        var bed = findByIdAndSchool(id);

        if (!bed.getRoomId().equals(request.getRoomId()) ||
                !bed.getBedNumber().equals(request.getBedNumber())) {
            if (bedRepository.existsBySchoolIdAndRoomIdAndBedNumber(bed.getSchoolId(), request.getRoomId(), request.getBedNumber())) {
                throw new ConflictException("Bed number already exists in this room");
            }
        }

        var room = roomRepository.findBySchoolIdAndId(bed.getSchoolId(), request.getRoomId())
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        bed.setRoomId(request.getRoomId());
        bed.setBedNumber(request.getBedNumber());
        if (request.getStatus() != null) {
            bed.setStatus(request.getStatus());
        }

        bedRepository.save(bed);

        auditLogService.log("BED_UPDATE", "Bed", bed.getId(),
                "Updated bed " + bed.getBedNumber() + " in room " + room.getRoomNumber(), null, null, null);

        return mapToDto(bed);
    }

    @Transactional
    public void deleteBed(Long id) {
        var bed = findByIdAndSchool(id);
        if ("OCCUPIED".equals(bed.getStatus())) {
            throw new BadRequestException("Cannot delete an occupied bed. Release it first.");
        }
        bed.setDeletedAt(java.time.LocalDateTime.now());
        bedRepository.save(bed);

        auditLogService.log("BED_DELETE", "Bed", bed.getId(),
                "Deleted bed " + bed.getBedNumber(), null, null, null);
    }

    private Bed findByIdAndSchool(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return bedRepository.findBySchoolIdAndId(tenantId, id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found"));
    }

    private BedDto mapToDto(Bed bed) {
        BedDto dto = new BedDto();
        dto.setId(bed.getId());
        dto.setUuid(bed.getUuid() != null ? bed.getUuid().toString() : null);
        dto.setSchoolId(bed.getSchoolId());
        dto.setRoomId(bed.getRoomId());
        dto.setBedNumber(bed.getBedNumber());
        dto.setStudentId(bed.getStudentId());
        dto.setStatus(bed.getStatus());
        dto.setAllocatedDate(bed.getAllocatedDate());
        dto.setReleasedDate(bed.getReleasedDate());
        dto.setCreatedAt(bed.getCreatedAt());
        dto.setUpdatedAt(bed.getUpdatedAt());
        return dto;
    }
}
