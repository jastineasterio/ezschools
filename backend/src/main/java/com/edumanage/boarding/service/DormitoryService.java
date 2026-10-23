package com.edumanage.boarding.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.boarding.dto.CreateDormitoryRequest;
import com.edumanage.boarding.dto.DormitoryDto;
import com.edumanage.boarding.entity.Dormitory;
import com.edumanage.boarding.repository.DormitoryRepository;
import com.edumanage.boarding.repository.RoomRepository;
import com.edumanage.users.repository.UserRepository;
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
public class DormitoryService {

    private final DormitoryRepository dormitoryRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public DormitoryService(DormitoryRepository dormitoryRepository, RoomRepository roomRepository,
                            UserRepository userRepository, AuditLogService auditLogService) {
        this.dormitoryRepository = dormitoryRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<DormitoryDto> listDormitories(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Dormitory> page;
        if (tenantId != null) {
            page = dormitoryRepository.findBySchoolId(tenantId, pageable);
        } else if (TenantContext.isSuperAdmin()) {
            page = dormitoryRepository.findAll(pageable);
        } else {
            throw new ForbiddenException("A school assignment is required");
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<DormitoryDto> listActiveDormitories() {
        Long tenantId = TenantContext.requireTenantId();
        return dormitoryRepository.findBySchoolIdAndStatus(tenantId, "ACTIVE").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public DormitoryDto getDormitory(Long id) {
        var dormitory = findByIdAndSchool(id);
        return mapToDto(dormitory);
    }

    @Transactional
    public DormitoryDto createDormitory(CreateDormitoryRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        validateWarden(request.getWardenId(), tenantId);

        if (dormitoryRepository.existsBySchoolIdAndName(tenantId, request.getName())) {
            throw new ConflictException("Dormitory with this name already exists");
        }

        Dormitory dormitory = new Dormitory();
        dormitory.setSchoolId(tenantId);
        dormitory.setName(request.getName());
        dormitory.setDescription(request.getDescription());
        dormitory.setCapacity(request.getCapacity() != null ? request.getCapacity() : 0);
        dormitory.setGender(request.getGender());
        dormitory.setWardenId(request.getWardenId());
        dormitory.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        dormitoryRepository.save(dormitory);

        auditLogService.log("DORMITORY_CREATE", "Dormitory", dormitory.getId(),
                "Created dormitory " + dormitory.getName(), null, null, null);

        return mapToDto(dormitory);
    }

    @Transactional
    public DormitoryDto updateDormitory(Long id, CreateDormitoryRequest request) {
        var dormitory = findByIdAndSchool(id);
        validateWarden(request.getWardenId(), dormitory.getSchoolId());

        if (!dormitory.getName().equals(request.getName()) &&
                dormitoryRepository.existsBySchoolIdAndName(dormitory.getSchoolId(), request.getName())) {
            throw new ConflictException("Dormitory with this name already exists");
        }

        dormitory.setName(request.getName());
        dormitory.setDescription(request.getDescription());
        if (request.getCapacity() != null) {
            dormitory.setCapacity(request.getCapacity());
        }
        dormitory.setGender(request.getGender());
        dormitory.setWardenId(request.getWardenId());
        if (request.getStatus() != null) {
            dormitory.setStatus(request.getStatus());
        }

        dormitoryRepository.save(dormitory);

        auditLogService.log("DORMITORY_UPDATE", "Dormitory", dormitory.getId(),
                "Updated dormitory " + dormitory.getName(), null, null, null);

        return mapToDto(dormitory);
    }

    @Transactional
    public void deleteDormitory(Long id) {
        var dormitory = findByIdAndSchool(id);
        boolean hasRooms = roomRepository.findBySchoolIdAndDormitoryId(dormitory.getSchoolId(), id).stream()
                .anyMatch(room -> !room.isDeleted());
        if (hasRooms) {
            throw new ConflictException("Delete or move the dormitory's rooms before deleting it");
        }
        dormitory.setDeletedAt(java.time.LocalDateTime.now());
        dormitoryRepository.save(dormitory);

        auditLogService.log("DORMITORY_DELETE", "Dormitory", dormitory.getId(),
                "Deleted dormitory " + dormitory.getName(), null, null, null);
    }

    private Dormitory findByIdAndSchool(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return dormitoryRepository.findBySchoolIdAndId(tenantId, id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));
    }

    private void validateWarden(Long wardenId, Long schoolId) {
        if (wardenId == null) {
            return;
        }
        userRepository.findById(wardenId)
                .filter(user -> !user.isDeleted() && schoolId.equals(user.getSchoolId()))
                .orElseThrow(() -> new ResourceNotFoundException("Warden not found in this school"));
    }

    private DormitoryDto mapToDto(Dormitory dormitory) {
        DormitoryDto dto = new DormitoryDto();
        dto.setId(dormitory.getId());
        dto.setUuid(dormitory.getUuid() != null ? dormitory.getUuid().toString() : null);
        dto.setSchoolId(dormitory.getSchoolId());
        dto.setName(dormitory.getName());
        dto.setDescription(dormitory.getDescription());
        dto.setCapacity(dormitory.getCapacity());
        dto.setGender(dormitory.getGender());
        dto.setWardenId(dormitory.getWardenId());
        dto.setStatus(dormitory.getStatus());
        dto.setCreatedAt(dormitory.getCreatedAt());
        dto.setUpdatedAt(dormitory.getUpdatedAt());
        return dto;
    }
}
