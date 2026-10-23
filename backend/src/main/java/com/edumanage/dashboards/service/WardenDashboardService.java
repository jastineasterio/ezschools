package com.edumanage.dashboards.service;

import com.edumanage.boarding.dto.BoardingAttendanceDto;
import com.edumanage.boarding.dto.BoardingStudentDto;
import com.edumanage.boarding.entity.BoardingAttendance;
import com.edumanage.boarding.entity.BoardingStudent;
import com.edumanage.boarding.entity.Dormitory;
import com.edumanage.boarding.entity.Room;
import com.edumanage.boarding.repository.BedRepository;
import com.edumanage.boarding.repository.BoardingAttendanceRepository;
import com.edumanage.boarding.repository.BoardingStudentRepository;
import com.edumanage.boarding.repository.DormitoryRepository;
import com.edumanage.boarding.repository.RoomRepository;
import com.edumanage.dashboards.dto.WardenDashboardDto;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.AccessGuard;
import com.edumanage.security.CurrentUserService;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.users.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the boarding warden dashboard for the warden's own school.
 */
@Service
public class WardenDashboardService {

    private static final int RECENT_LIMIT = 10;
    private static final int MAX_ROWS = 500;

    private final AccessGuard accessGuard;
    private final CurrentUserService currentUserService;
    private final DormitoryRepository dormitoryRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final BoardingStudentRepository boardingStudentRepository;
    private final BoardingAttendanceRepository boardingAttendanceRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SchoolRepository schoolRepository;

    public WardenDashboardService(AccessGuard accessGuard,
                                  CurrentUserService currentUserService,
                                  DormitoryRepository dormitoryRepository,
                                  RoomRepository roomRepository,
                                  BedRepository bedRepository,
                                  BoardingStudentRepository boardingStudentRepository,
                                  BoardingAttendanceRepository boardingAttendanceRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  SchoolRepository schoolRepository) {
        this.accessGuard = accessGuard;
        this.currentUserService = currentUserService;
        this.dormitoryRepository = dormitoryRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.boardingStudentRepository = boardingStudentRepository;
        this.boardingAttendanceRepository = boardingAttendanceRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.schoolRepository = schoolRepository;
    }

    @Transactional(readOnly = true)
    public WardenDashboardDto getDashboard() {
        accessGuard.assertAnyRole("WARDEN", "MANAGER", "SUPER_ADMIN");

        User user = currentUserService.requireCurrentUser();
        Long schoolId = user.getSchoolId();
        if (schoolId == null) {
            throw new ForbiddenException("A school assignment is required");
        }

        WardenDashboardDto dto = new WardenDashboardDto();
        dto.setSchoolId(schoolId);
        dto.setSchoolName(schoolRepository.findById(schoolId).map(school -> school.getName()).orElse(null));

        List<Dormitory> dormitories = dormitoryRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_ROWS)).getContent();
        dto.setTotalDormitories(dormitories.size());
        dto.setAssignedDormitories(dormitoryRepository.findBySchoolIdAndWardenId(schoolId, user.getId()).stream()
                .filter(dormitory -> !dormitory.isDeleted())
                .count());

        dto.setTotalRooms(roomRepository.findBySchoolId(schoolId, PageRequest.of(0, 1)).getTotalElements());
        dto.setTotalBeds(bedRepository.findBySchoolId(schoolId, PageRequest.of(0, 1)).getTotalElements());
        dto.setOccupiedBeds(bedRepository.countBySchoolIdAndStatus(schoolId, "OCCUPIED"));

        List<BoardingStudent> boardingStudents = boardingStudentRepository.findBySchoolIdAndStatus(schoolId, "ACTIVE");
        dto.setBoardingStudents(boardingStudents.size());
        dto.setStudents(boardingStudents.stream()
                .filter(student -> !student.isDeleted())
                .map(this::mapBoardingStudent)
                .toList());

        LocalDate today = LocalDate.now();
        dto.setAttendanceDate(today);
        List<BoardingAttendance> todayAttendance = boardingAttendanceRepository.findBySchoolIdAndAttendanceDate(schoolId, today).stream()
                .filter(attendance -> !attendance.isDeleted())
                .toList();
        dto.setPresentToday(todayAttendance.stream()
                .filter(attendance -> "PRESENT".equalsIgnoreCase(attendance.getStatus())
                        || "LATE".equalsIgnoreCase(attendance.getStatus()))
                .count());
        dto.setAbsentToday(todayAttendance.stream()
                .filter(attendance -> "ABSENT".equalsIgnoreCase(attendance.getStatus()))
                .count());

        dto.setRecentAttendance(boardingAttendanceRepository
                .findBySchoolId(schoolId, PageRequest.of(0, RECENT_LIMIT, Sort.by(Sort.Direction.DESC, "attendanceDate")))
                .getContent().stream()
                .filter(attendance -> !attendance.isDeleted())
                .map(this::mapBoardingAttendance)
                .toList());

        return dto;
    }

    private BoardingStudentDto mapBoardingStudent(BoardingStudent entity) {
        BoardingStudentDto dto = new BoardingStudentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());
        dto.setDormitoryId(entity.getDormitoryId());
        dto.setRoomId(entity.getRoomId());
        dto.setBedId(entity.getBedId());
        dto.setAllocatedDate(entity.getAllocatedDate());
        dto.setReleasedDate(entity.getReleasedDate());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        studentProfileRepository.findById(entity.getStudentId()).ifPresent(student -> {
            dto.setStudentName(student.getFirstName() + " " + student.getLastName());
            dto.setAdmissionNumber(student.getAdmissionNumber());
        });
        if (entity.getDormitoryId() != null) {
            dormitoryRepository.findById(entity.getDormitoryId()).ifPresent(dormitory -> dto.setDormitoryName(dormitory.getName()));
        }
        if (entity.getRoomId() != null) {
            roomRepository.findById(entity.getRoomId()).ifPresent(room -> dto.setRoomNumber(room.getRoomNumber()));
        }
        if (entity.getBedId() != null) {
            bedRepository.findById(entity.getBedId()).ifPresent(bed -> dto.setBedNumber(bed.getBedNumber()));
        }
        return dto;
    }

    private BoardingAttendanceDto mapBoardingAttendance(BoardingAttendance entity) {
        BoardingAttendanceDto dto = new BoardingAttendanceDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());
        dto.setDormitoryId(entity.getDormitoryId());
        dto.setAttendanceDate(entity.getAttendanceDate());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        studentProfileRepository.findById(entity.getStudentId()).ifPresent(student ->
                dto.setStudentName(student.getFirstName() + " " + student.getLastName()));
        if (entity.getDormitoryId() != null) {
            dormitoryRepository.findById(entity.getDormitoryId()).ifPresent(dormitory -> dto.setDormitoryName(dormitory.getName()));
        }
        return dto;
    }
}
