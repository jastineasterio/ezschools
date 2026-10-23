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
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.security.AccessGuard;
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
    private final AccessGuard accessGuard;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StreamSubjectAssignmentRepository streamSubjectAssignmentRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             StudentProfileRepository studentProfileRepository,
                             ClassRepository classRepository,
                             StreamRepository streamRepository,
                             AuditLogService auditLogService,
                             AccessGuard accessGuard,
                             StudentEnrollmentRepository enrollmentRepository,
                             StreamSubjectAssignmentRepository streamSubjectAssignmentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
        this.enrollmentRepository = enrollmentRepository;
        this.streamSubjectAssignmentRepository = streamSubjectAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceDto> list(Pageable pageable) {
        return list(pageable, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceDto> list(Pageable pageable, Long classId, LocalDate date) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null && !TenantContext.isSuperAdmin()) {
            throw new ForbiddenException("A school assignment is required");
        }
        if (classId != null) {
            var clazz = classRepository.findById(classId)
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
            if (tenantId != null && !tenantId.equals(clazz.getSchoolId())) {
                throw new ForbiddenException("Access denied");
            }
        }

        Page<Attendance> page;
        if (tenantId != null) {
            if (classId != null && date != null) {
                page = attendanceRepository.findBySchoolIdAndClassIdAndAttendanceDateAndDeletedAtIsNull(
                        tenantId, classId, date, pageable);
            } else if (classId != null) {
                page = attendanceRepository.findBySchoolIdAndClassIdAndDeletedAtIsNull(
                        tenantId, classId, pageable);
            } else if (date != null) {
                page = attendanceRepository.findBySchoolIdAndAttendanceDateAndDeletedAtIsNull(
                        tenantId, date, pageable);
            } else {
                page = attendanceRepository.findBySchoolIdAndDeletedAtIsNull(tenantId, pageable);
            }
        } else {
            if (classId != null && date != null) {
                page = attendanceRepository.findByClassIdAndAttendanceDateAndDeletedAtIsNull(
                        classId, date, pageable);
            } else if (classId != null) {
                page = attendanceRepository.findByClassIdAndDeletedAtIsNull(classId, pageable);
            } else if (date != null) {
                page = attendanceRepository.findByAttendanceDateAndDeletedAtIsNull(date, pageable);
            } else {
                page = attendanceRepository.findByDeletedAtIsNull(pageable);
            }
        }
        return PageResponse.from(page.map(attendance -> mapToDto(attendance, null, null, null, null, null, null)));
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<AttendanceDto> listByStudent(Long studentId) {
        accessGuard.assertCanAccessStudent(studentId);
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

        validateEnrollmentContext(request, tenantId);

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
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var attendance = attendanceRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

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

        validateEnrollmentContext(request, tenantId);

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
        var student = entity.getStudent();
        dto.setStudentName(studentName != null ? studentName
                : student == null ? null : student.getFirstName() + " " + student.getLastName());
        dto.setClassId(entity.getClassId());
        dto.setClassName(className != null ? className
                : entity.getClazz() == null ? null : entity.getClazz().getName());
        dto.setStreamId(entity.getStreamId());
        dto.setStreamName(streamName != null ? streamName
                : entity.getStream() == null ? null : entity.getStream().getName());
        dto.setAcademicYearId(entity.getAcademicYearId());
        dto.setAcademicYearName(academicYearName != null ? academicYearName
                : entity.getAcademicYear() == null ? null : entity.getAcademicYear().getName());
        dto.setSubjectId(entity.getSubjectId());
        dto.setSubjectName(subjectName != null ? subjectName
                : entity.getSubject() == null ? null : entity.getSubject().getName());
        dto.setTeacherId(entity.getTeacherId());
        var teacher = entity.getTeacher();
        dto.setTeacherName(teacherName != null ? teacherName
                : teacher == null ? null : teacher.getFirstName() + " " + teacher.getLastName());
        dto.setAttendanceDate(entity.getAttendanceDate());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private void validateEnrollmentContext(CreateAttendanceRequest request, Long schoolId) {
        if (request.getStreamId() != null) {
            var stream = streamRepository.findById(request.getStreamId())
                    .filter(value -> !value.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (!schoolId.equals(stream.getSchoolId())) {
                throw new ForbiddenException("Stream does not belong to this school");
            }
            if (!request.getClassId().equals(stream.getClassId())) {
                throw new BadRequestException("Stream does not belong to the selected class");
            }
            if (request.getSubjectId() != null
                    && !streamSubjectAssignmentRepository.existsBySchoolIdAndClassIdAndStreamIdAndAcademicYearIdAndSubjectId(
                    schoolId, request.getClassId(), request.getStreamId(), request.getAcademicYearId(),
                    request.getSubjectId())) {
                throw new BadRequestException("Subject is not configured for the selected class stream");
            }
        }

        var enrollment = enrollmentRepository.findByStudentIdAndAcademicYearId(
                        request.getStudentId(), request.getAcademicYearId())
                .filter(value -> !value.isDeleted() && "ENROLLED".equals(value.getStatus()))
                .orElseThrow(() -> new BadRequestException(
                        "Student is not enrolled for the selected academic year"));
        if (!schoolId.equals(enrollment.getSchoolId())) {
            throw new ForbiddenException("Student enrollment does not belong to this school");
        }
        if (!request.getClassId().equals(enrollment.getClassId())
                || (request.getStreamId() != null && !request.getStreamId().equals(enrollment.getStreamId()))) {
            throw new BadRequestException("Attendance class and stream must match the student's enrollment");
        }
    }
}
