package com.edumanage.attendance.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.attendance.dto.AttendanceDto;
import com.edumanage.attendance.dto.CreateAttendanceRequest;
import com.edumanage.attendance.entity.Attendance;
import com.edumanage.attendance.repository.AttendanceRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final AuditLogService auditLogService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             StudentProfileRepository studentProfileRepository,
                             ClassRepository classRepository,
                             StreamRepository streamRepository,
                             AuditLogService auditLogService) {
        this.attendanceRepository = attendanceRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<AttendanceDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Attendance> page;
        if (tenantId != null) {
            page = attendanceRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = attendanceRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(attendance -> mapToDto(attendance, null, null, null, null, null, null)));
    }

    public List<AttendanceDto> listByClassAndDate(Long classId, LocalDate date) {
        var clazz = classRepository.findById(classId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return attendanceRepository.findBySchoolIdAndAttendanceDate(clazz.getSchoolId(), date).stream()
                .filter(a -> a.getClassId().equals(classId))
                .map(attendance -> mapToDto(attendance, null, clazz.getName(), null, null, null, null))
                .collect(Collectors.toList());
    }

    public List<AttendanceDto> listByStudent(Long studentId) {
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return attendanceRepository.findByStudentIdAndSchoolId(studentId, tenantId != null ? tenantId : null).stream()
                .map(attendance -> mapToDto(attendance, student.getFirstName() + " " + student.getLastName(), null, null, null, null, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceDto create(CreateAttendanceRequest request) {
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

        var clazz = classRepository.findById(request.getClassId())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!tenantId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Class does not belong to this school");
        }

        if (request.getStreamId() != null) {
            var stream = streamRepository.findById(request.getStreamId())
                    .filter(s -> !s.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (!tenantId.equals(stream.getSchoolId())) {
                throw new ForbiddenException("Stream does not belong to this school");
            }
        }

        if (attendanceRepository.existsBySchoolIdAndStudentIdAndClassIdAndAttendanceDateAndSubjectId(
                tenantId, request.getStudentId(), request.getClassId(), request.getAttendanceDate(), request.getSubjectId())) {
            throw new com.edumanage.exception.ConflictException("Attendance already recorded for this student");
        }

        Attendance attendance = new Attendance();
        attendance.setSchoolId(tenantId);
        attendance.setStudentId(request.getStudentId());
        attendance.setClassId(request.getClassId());
        attendance.setStreamId(request.getStreamId());
        attendance.setAcademicYearId(request.getAcademicYearId());
        attendance.setSubjectId(request.getSubjectId());
        attendance.setTeacherId(request.getTeacherId());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());

        attendanceRepository.save(attendance);

        auditLogService.log("ATTENDANCE_CREATE", "Attendance", attendance.getId(),
                "Created attendance record for student", null, null, null);

        return mapToDto(attendance, student.getFirstName() + " " + student.getLastName(), clazz.getName(), null, null, null, null);
    }

    @Transactional
    public List<AttendanceDto> bulkCreate(List<CreateAttendanceRequest> requests) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        List<AttendanceDto> results = new ArrayList<>();
        for (CreateAttendanceRequest request : requests) {
            AttendanceDto dto = create(request);
            results.add(dto);
        }
        return results;
    }

    @Transactional
    public AttendanceDto update(Long id, CreateAttendanceRequest request) {
        var attendance = attendanceRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(attendance.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var clazz = classRepository.findById(request.getClassId())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!tenantId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Class does not belong to this school");
        }

        attendance.setStudentId(request.getStudentId());
        attendance.setClassId(request.getClassId());
        attendance.setStreamId(request.getStreamId());
        attendance.setAcademicYearId(request.getAcademicYearId());
        attendance.setSubjectId(request.getSubjectId());
        attendance.setTeacherId(request.getTeacherId());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());

        attendanceRepository.save(attendance);

        auditLogService.log("ATTENDANCE_UPDATE", "Attendance", attendance.getId(),
                "Updated attendance record", null, null, null);

        return mapToDto(attendance, student.getFirstName() + " " + student.getLastName(), clazz.getName(), null, null, null, null);
    }

    private AttendanceDto mapToDto(Attendance entity, String studentName, String className,
                                   String streamName, String academicYearName, String subjectName, String teacherName) {
        AttendanceDto dto = new AttendanceDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());
        dto.setStudentName(studentName);
        dto.setClassId(entity.getClassId());
        dto.setClassName(className);
        dto.setStreamId(entity.getStreamId());
        dto.setStreamName(streamName);
        dto.setAcademicYearId(entity.getAcademicYearId());
        dto.setAcademicYearName(academicYearName);
        dto.setSubjectId(entity.getSubjectId());
        dto.setSubjectName(subjectName);
        dto.setTeacherId(entity.getTeacherId());
        dto.setTeacherName(teacherName);
        dto.setAttendanceDate(entity.getAttendanceDate());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
