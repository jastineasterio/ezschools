package com.edumanage.enrollments.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.enrollments.dto.CreateStudentEnrollmentRequest;
import com.edumanage.enrollments.dto.StudentEnrollmentDto;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentEnrollmentService {

    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final AuditLogService auditLogService;

    public StudentEnrollmentService(StudentEnrollmentRepository studentEnrollmentRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    AcademicYearRepository academicYearRepository,
                                    ClassRepository classRepository,
                                    StreamRepository streamRepository,
                                    AuditLogService auditLogService) {
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.academicYearRepository = academicYearRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<StudentEnrollmentDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<StudentEnrollment> page;
        if (tenantId != null) {
            page = studentEnrollmentRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = studentEnrollmentRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(enrollment -> mapToDto(enrollment, null, null, null, null)));
    }

    public List<StudentEnrollmentDto> listByStudent(Long studentId) {
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return studentEnrollmentRepository.findByStudentIdAndSchoolId(studentId, tenantId != null ? tenantId : null).stream()
                .map(enrollment -> mapToDto(enrollment, null, null, null, null))
                .collect(java.util.stream.Collectors.toList());
    }

    public StudentEnrollmentDto getById(Long id) {
        var enrollment = studentEnrollmentRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(enrollment.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(enrollment, null, null, null, null);
    }

    @Transactional
    public StudentEnrollmentDto create(CreateStudentEnrollmentRequest request) {
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

        var academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        if (!tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Academic year does not belong to this school");
        }

        var clazz = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!tenantId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Class does not belong to this school");
        }

        if (request.getStreamId() != null) {
            var stream = streamRepository.findById(request.getStreamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (!tenantId.equals(stream.getSchoolId())) {
                throw new ForbiddenException("Stream does not belong to this school");
            }
        }

        if (studentEnrollmentRepository.findByStudentIdAndAcademicYearId(request.getStudentId(), request.getAcademicYearId()).isPresent()) {
            throw new com.edumanage.exception.ConflictException("Student already enrolled for this academic year");
        }

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(request.getStudentId());
        enrollment.setSchoolId(tenantId);
        enrollment.setAcademicYearId(request.getAcademicYearId());
        enrollment.setClassId(request.getClassId());
        enrollment.setStreamId(request.getStreamId());
        enrollment.setStatus(request.getStatus() != null ? request.getStatus() : "ENROLLED");
        enrollment.setStartDate(request.getStartDate());
        enrollment.setEndDate(request.getEndDate());

        studentEnrollmentRepository.save(enrollment);

        auditLogService.log("ENROLLMENT_CREATE", "StudentEnrollment", enrollment.getId(),
                "Created enrollment for student", null, null, null);

        return mapToDto(enrollment, student.getFirstName() + " " + student.getLastName(), academicYear.getName(), clazz.getName(), null);
    }

    @Transactional
    public StudentEnrollmentDto update(Long id, CreateStudentEnrollmentRequest request) {
        var enrollment = studentEnrollmentRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(enrollment.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        if (!tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Academic year does not belong to this school");
        }

        var clazz = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!tenantId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Class does not belong to this school");
        }

        if (request.getStreamId() != null) {
            var stream = streamRepository.findById(request.getStreamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (!tenantId.equals(stream.getSchoolId())) {
                throw new ForbiddenException("Stream does not belong to this school");
            }
        }

        enrollment.setStudentId(request.getStudentId());
        enrollment.setAcademicYearId(request.getAcademicYearId());
        enrollment.setClassId(request.getClassId());
        enrollment.setStreamId(request.getStreamId());
        if (request.getStatus() != null) {
            enrollment.setStatus(request.getStatus());
        }
        enrollment.setStartDate(request.getStartDate());
        enrollment.setEndDate(request.getEndDate());

        studentEnrollmentRepository.save(enrollment);

        auditLogService.log("ENROLLMENT_UPDATE", "StudentEnrollment", enrollment.getId(),
                "Updated enrollment for student", null, null, null);

        return mapToDto(enrollment, student.getFirstName() + " " + student.getLastName(), academicYear.getName(), clazz.getName(), null);
    }

    private StudentEnrollmentDto mapToDto(StudentEnrollment entity, String studentName,
                                          String academicYearName, String className, String streamName) {
        StudentEnrollmentDto dto = new StudentEnrollmentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setStudentId(entity.getStudentId());
        dto.setStudentName(studentName);
        dto.setSchoolId(entity.getSchoolId());
        dto.setAcademicYearId(entity.getAcademicYearId());
        dto.setAcademicYearName(academicYearName);
        dto.setClassId(entity.getClassId());
        dto.setClassName(className);
        dto.setStreamId(entity.getStreamId());
        dto.setStreamName(streamName);
        dto.setStatus(entity.getStatus());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
