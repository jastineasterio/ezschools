package com.edumanage.boarding.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.boarding.dto.BoardingAttendanceDto;
import com.edumanage.boarding.dto.CreateBoardingAttendanceRequest;
import com.edumanage.boarding.entity.BoardingAttendance;
import com.edumanage.boarding.entity.Dormitory;
import com.edumanage.boarding.repository.BoardingAttendanceRepository;
import com.edumanage.boarding.repository.DormitoryRepository;
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
public class BoardingAttendanceService {

    private final BoardingAttendanceRepository boardingAttendanceRepository;
    private final DormitoryRepository dormitoryRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditLogService auditLogService;

    public BoardingAttendanceService(BoardingAttendanceRepository boardingAttendanceRepository,
                                     DormitoryRepository dormitoryRepository,
                                     StudentProfileRepository studentProfileRepository,
                                     AuditLogService auditLogService) {
        this.boardingAttendanceRepository = boardingAttendanceRepository;
        this.dormitoryRepository = dormitoryRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<BoardingAttendanceDto> listAttendance(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<BoardingAttendance> page;
        if (tenantId != null) {
            page = boardingAttendanceRepository.findBySchoolId(tenantId, pageable);
        } else if (TenantContext.isSuperAdmin()) {
            page = boardingAttendanceRepository.findAll(pageable);
        } else {
            throw new ForbiddenException("A school assignment is required");
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<BoardingAttendanceDto> listAttendanceByDormitoryAndDate(Long dormitoryId, LocalDate date) {
        Long tenantId = TenantContext.requireTenantId();
        var dormitory = dormitoryRepository.findBySchoolIdAndId(tenantId, dormitoryId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));

        return boardingAttendanceRepository.findBySchoolIdAndDormitoryIdAndAttendanceDate(tenantId, dormitoryId, date).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public BoardingAttendanceDto getAttendance(Long id) {
        var record = findByIdAndSchool(id);
        return mapToDto(record);
    }

    @Transactional
    public BoardingAttendanceDto createAttendance(CreateBoardingAttendanceRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var dormitory = dormitoryRepository.findBySchoolIdAndId(tenantId, request.getDormitoryId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));

        if (boardingAttendanceRepository.existsBySchoolIdAndStudentIdAndDormitoryIdAndAttendanceDate(
                tenantId, request.getStudentId(), request.getDormitoryId(), request.getAttendanceDate())) {
            throw new ConflictException("Attendance already recorded for this student on this date");
        }

        BoardingAttendance attendance = new BoardingAttendance();
        attendance.setSchoolId(tenantId);
        attendance.setStudentId(request.getStudentId());
        attendance.setDormitoryId(request.getDormitoryId());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());

        boardingAttendanceRepository.save(attendance);

        auditLogService.log("BOARDING_ATTENDANCE_CREATE", "BoardingAttendance", attendance.getId(),
                "Recorded boarding attendance for student " + student.getFirstName(), null, null, null);

        return mapToDto(attendance);
    }

    @Transactional
    public BoardingAttendanceDto updateAttendance(Long id, CreateBoardingAttendanceRequest request) {
        var attendance = findByIdAndSchool(id);

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!attendance.getSchoolId().equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var dormitory = dormitoryRepository.findBySchoolIdAndId(attendance.getSchoolId(), request.getDormitoryId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Dormitory not found"));

        if ((!attendance.getStudentId().equals(request.getStudentId())
                || !attendance.getDormitoryId().equals(request.getDormitoryId())
                || !attendance.getAttendanceDate().equals(request.getAttendanceDate()))
                && boardingAttendanceRepository.existsBySchoolIdAndStudentIdAndDormitoryIdAndAttendanceDate(
                attendance.getSchoolId(), request.getStudentId(), request.getDormitoryId(), request.getAttendanceDate())) {
            throw new ConflictException("Attendance already recorded for this student on this date");
        }

        attendance.setStudentId(request.getStudentId());
        attendance.setDormitoryId(request.getDormitoryId());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());

        boardingAttendanceRepository.save(attendance);

        auditLogService.log("BOARDING_ATTENDANCE_UPDATE", "BoardingAttendance", attendance.getId(),
                "Updated boarding attendance", null, null, null);

        return mapToDto(attendance);
    }

    @Transactional
    public void deleteAttendance(Long id) {
        var attendance = findByIdAndSchool(id);
        attendance.setDeletedAt(java.time.LocalDateTime.now());
        boardingAttendanceRepository.save(attendance);

        auditLogService.log("BOARDING_ATTENDANCE_DELETE", "BoardingAttendance", attendance.getId(),
                "Deleted boarding attendance", null, null, null);
    }

    private BoardingAttendance findByIdAndSchool(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }
        return boardingAttendanceRepository.findBySchoolIdAndId(tenantId, id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Boarding attendance not found"));
    }

    private BoardingAttendanceDto mapToDto(BoardingAttendance attendance) {
        BoardingAttendanceDto dto = new BoardingAttendanceDto();
        dto.setId(attendance.getId());
        dto.setUuid(attendance.getUuid() != null ? attendance.getUuid().toString() : null);
        dto.setSchoolId(attendance.getSchoolId());
        dto.setStudentId(attendance.getStudentId());
        dto.setDormitoryId(attendance.getDormitoryId());
        dto.setAttendanceDate(attendance.getAttendanceDate());
        dto.setStatus(attendance.getStatus());
        dto.setRemarks(attendance.getRemarks());
        dto.setCreatedAt(attendance.getCreatedAt());
        dto.setUpdatedAt(attendance.getUpdatedAt());
        return dto;
    }
}
