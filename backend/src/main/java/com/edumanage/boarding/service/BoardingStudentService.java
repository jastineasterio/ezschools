package com.edumanage.boarding.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.boarding.dto.CreateBoardingStudentRequest;
import com.edumanage.boarding.dto.BoardingStudentDto;
import com.edumanage.boarding.entity.Bed;
import com.edumanage.boarding.entity.BoardingStudent;
import com.edumanage.boarding.entity.Dormitory;
import com.edumanage.boarding.entity.Room;
import com.edumanage.boarding.repository.BedRepository;
import com.edumanage.boarding.repository.BoardingStudentRepository;
import com.edumanage.boarding.repository.DormitoryRepository;
import com.edumanage.boarding.repository.RoomRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BoardingStudentService {

    private final BoardingStudentRepository boardingStudentRepository;
    private final BedRepository bedRepository;
    private final RoomRepository roomRepository;
    private final DormitoryRepository dormitoryRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditLogService auditLogService;

    public BoardingStudentService(BoardingStudentRepository boardingStudentRepository,
                                  BedRepository bedRepository,
                                  RoomRepository roomRepository,
                                  DormitoryRepository dormitoryRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  AuditLogService auditLogService) {
        this.boardingStudentRepository = boardingStudentRepository;
        this.bedRepository = bedRepository;
        this.roomRepository = roomRepository;
        this.dormitoryRepository = dormitoryRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<BoardingStudentDto> listBoardingStudents(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<BoardingStudent> page;
        if (tenantId != null) {
            page = boardingStudentRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = boardingStudentRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<BoardingStudentDto> listActiveBoardingStudents() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return boardingStudentRepository.findBySchoolIdAndStatus(tenantId, "ACTIVE").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public BoardingStudentDto getBoardingStudent(Long id) {
        var record = findByIdAndSchool(id);
        return mapToDto(record);
    }

    @Transactional
    public BoardingStudentDto allocateStudent(CreateBoardingStudentRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        if (boardingStudentRepository.existsBySchoolIdAndStudentIdAndStatus(tenantId, request.getStudentId(), "ACTIVE")) {
            throw new ConflictException("Student is already allocated to a boarding facility");
        }

        var dormitory = dormitoryRepository.findBySchoolIdAndId(tenantId, request.getDormitoryId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));

        var room = roomRepository.findBySchoolIdAndId(tenantId, request.getRoomId())
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!room.getDormitoryId().equals(request.getDormitoryId())) {
            throw new BadRequestException("Room does not belong to the selected dormitory");
        }

        var bed = bedRepository.findBySchoolIdAndId(tenantId, request.getBedId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found"));

        if (!bed.getRoomId().equals(request.getRoomId())) {
            throw new BadRequestException("Bed does not belong to the selected room");
        }

        if ("OCCUPIED".equals(bed.getStatus())) {
            throw new ConflictException("Bed is already occupied");
        }

        if (room.getCurrentOccupancy() >= room.getCapacity()) {
            throw new BadRequestException("Room is at full capacity");
        }

        bed.setStatus("OCCUPIED");
        bed.setStudentId(request.getStudentId());
        bed.setAllocatedDate(LocalDate.now());
        bedRepository.save(bed);

        room.setCurrentOccupancy(room.getCurrentOccupancy() + 1);
        roomRepository.save(room);

        BoardingStudent boardingStudent = new BoardingStudent();
        boardingStudent.setSchoolId(tenantId);
        boardingStudent.setStudentId(request.getStudentId());
        boardingStudent.setDormitoryId(request.getDormitoryId());
        boardingStudent.setRoomId(request.getRoomId());
        boardingStudent.setBedId(request.getBedId());
        boardingStudent.setAllocatedDate(LocalDate.now());
        boardingStudent.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        boardingStudentRepository.save(boardingStudent);

        auditLogService.log("BOARDING_ALLOCATE", "BoardingStudent", boardingStudent.getId(),
                "Allocated student " + student.getFirstName() + " to dormitory " + dormitory.getName(), null, null, null);

        return mapToDto(boardingStudent);
    }

    @Transactional
    public BoardingStudentDto transferStudent(Long id, CreateBoardingStudentRequest request) {
        var existing = findByIdAndSchool(id);

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        var oldBed = bedRepository.findBySchoolIdAndId(existing.getSchoolId(), existing.getBedId())
                .orElseThrow(() -> new ResourceNotFoundException("Old bed not found"));
        var oldRoom = roomRepository.findBySchoolIdAndId(existing.getSchoolId(), existing.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Old room not found"));

        oldBed.setStatus("AVAILABLE");
        oldBed.setStudentId(null);
        oldBed.setReleasedDate(LocalDate.now());
        bedRepository.save(oldBed);

        oldRoom.setCurrentOccupancy(Math.max(0, oldRoom.getCurrentOccupancy() - 1));
        roomRepository.save(oldRoom);

        var newDormitory = dormitoryRepository.findBySchoolIdAndId(existing.getSchoolId(), request.getDormitoryId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));

        var newRoom = roomRepository.findBySchoolIdAndId(existing.getSchoolId(), request.getRoomId())
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!newRoom.getDormitoryId().equals(request.getDormitoryId())) {
            throw new BadRequestException("Room does not belong to the selected dormitory");
        }

        var newBed = bedRepository.findBySchoolIdAndId(existing.getSchoolId(), request.getBedId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found"));

        if (!newBed.getRoomId().equals(request.getRoomId())) {
            throw new BadRequestException("Bed does not belong to the selected room");
        }

        if ("OCCUPIED".equals(newBed.getStatus())) {
            throw new ConflictException("Bed is already occupied");
        }

        if (newRoom.getCurrentOccupancy() >= newRoom.getCapacity()) {
            throw new BadRequestException("Room is at full capacity");
        }

        newBed.setStatus("OCCUPIED");
        newBed.setStudentId(request.getStudentId());
        newBed.setAllocatedDate(LocalDate.now());
        bedRepository.save(newBed);

        newRoom.setCurrentOccupancy(newRoom.getCurrentOccupancy() + 1);
        roomRepository.save(newRoom);

        existing.setDormitoryId(request.getDormitoryId());
        existing.setRoomId(request.getRoomId());
        existing.setBedId(request.getBedId());
        existing.setReleasedDate(LocalDate.now());
        existing.setAllocatedDate(LocalDate.now());

        boardingStudentRepository.save(existing);

        auditLogService.log("BOARDING_TRANSFER", "BoardingStudent", existing.getId(),
                "Transferred student " + student.getFirstName() + " to dormitory " + newDormitory.getName(), null, null, null);

        return mapToDto(existing);
    }

    @Transactional
    public BoardingStudentDto releaseStudent(Long id) {
        var record = findByIdAndSchool(id);

        if (!"ACTIVE".equals(record.getStatus())) {
            throw new BadRequestException("Only active boarding records can be released");
        }

        var bed = bedRepository.findBySchoolIdAndId(record.getSchoolId(), record.getBedId())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found"));
        var room = roomRepository.findBySchoolIdAndId(record.getSchoolId(), record.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        bed.setStatus("AVAILABLE");
        bed.setStudentId(null);
        bed.setReleasedDate(LocalDate.now());
        bedRepository.save(bed);

        room.setCurrentOccupancy(Math.max(0, room.getCurrentOccupancy() - 1));
        roomRepository.save(room);

        record.setStatus("RELEASED");
        record.setReleasedDate(LocalDate.now());
        boardingStudentRepository.save(record);

        auditLogService.log("BOARDING_RELEASE", "BoardingStudent", record.getId(),
                "Released student from boarding", null, null, null);

        return mapToDto(record);
    }

    private BoardingStudent findByIdAndSchool(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return boardingStudentRepository.findBySchoolIdAndId(tenantId, id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Boarding record not found"));
    }

    private BoardingStudentDto mapToDto(BoardingStudent record) {
        BoardingStudentDto dto = new BoardingStudentDto();
        dto.setId(record.getId());
        dto.setUuid(record.getUuid() != null ? record.getUuid().toString() : null);
        dto.setSchoolId(record.getSchoolId());
        dto.setStudentId(record.getStudentId());
        dto.setDormitoryId(record.getDormitoryId());
        dto.setRoomId(record.getRoomId());
        dto.setBedId(record.getBedId());
        dto.setAllocatedDate(record.getAllocatedDate());
        dto.setReleasedDate(record.getReleasedDate());
        dto.setStatus(record.getStatus());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }
}
