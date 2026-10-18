package com.edumanage.teacherassignments.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.teacherassignments.dto.CreateTeacherAssignmentRequest;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.academicyears.repository.AcademicYearRepository;
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
public class TeacherAssignmentService {

    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AuditLogService auditLogService;

    public TeacherAssignmentService(TeacherAssignmentRepository teacherAssignmentRepository, UserRepository userRepository,
                                    SubjectRepository subjectRepository, ClassRepository classRepository,
                                    StreamRepository streamRepository, AcademicYearRepository academicYearRepository,
                                    AuditLogService auditLogService) {
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.academicYearRepository = academicYearRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<TeacherAssignmentDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<TeacherAssignment> page;
        if (tenantId != null) {
            page = teacherAssignmentRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = teacherAssignmentRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(assignment -> mapToDto(assignment, null, null, null, null, null)));
    }

    public List<TeacherAssignmentDto> listByTeacher(Long teacherId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            var teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
            if (!tenantId.equals(teacher.getSchoolId())) {
                throw new ForbiddenException("Access denied");
            }
        }

        return teacherAssignmentRepository.findByTeacherIdAndSchoolId(teacherId, tenantId != null ? tenantId : null).stream()
                .map(assignment -> mapToDto(assignment, null, null, null, null, null))
                .collect(Collectors.toList());
    }

    public TeacherAssignmentDto getById(Long id) {
        var assignment = teacherAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(assignment.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(assignment, null, null, null, null, null);
    }

    @Transactional
    public TeacherAssignmentDto create(CreateTeacherAssignmentRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        boolean hasTeacherRole = teacher.getRoles().stream()
                .anyMatch(role -> "TEACHER".equals(role.getName()));
        if (!hasTeacherRole) {
            throw new ForbiddenException("User does not have TEACHER role");
        }

        if (!tenantId.equals(teacher.getSchoolId())) {
            throw new ForbiddenException("Teacher does not belong to this school");
        }

        var subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!tenantId.equals(subject.getSchoolId())) {
            throw new ForbiddenException("Subject does not belong to this school");
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

        var academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        if (!tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Academic year does not belong to this school");
        }

        if (teacherAssignmentRepository.existsByTeacherIdAndSubjectIdAndClassIdAndAcademicYearId(
                request.getTeacherId(), request.getSubjectId(), request.getClassId(), request.getAcademicYearId())) {
            throw new ConflictException("Teacher assignment already exists");
        }

        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setSchoolId(tenantId);
        assignment.setTeacherId(request.getTeacherId());
        assignment.setSubjectId(request.getSubjectId());
        assignment.setClassId(request.getClassId());
        assignment.setStreamId(request.getStreamId());
        assignment.setAcademicYearId(request.getAcademicYearId());
        assignment.setClassTeacher(request.isClassTeacher());

        teacherAssignmentRepository.save(assignment);

        auditLogService.log("TEACHER_ASSIGNMENT_CREATE", "TeacherAssignment", assignment.getId(),
                "Created teacher assignment for teacher " + teacher.getEmail(), null, null, null);

        return mapToDto(assignment, teacher.getFirstName() + " " + teacher.getLastName(), subject.getName(), clazz.getName(), null, academicYear.getName());
    }

    @Transactional
    public TeacherAssignmentDto update(Long id, CreateTeacherAssignmentRequest request) {
        var assignment = teacherAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(assignment.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        boolean hasTeacherRole = teacher.getRoles().stream()
                .anyMatch(role -> "TEACHER".equals(role.getName()));
        if (!hasTeacherRole) {
            throw new ForbiddenException("User does not have TEACHER role");
        }

        var subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!tenantId.equals(subject.getSchoolId())) {
            throw new ForbiddenException("Subject does not belong to this school");
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

        var academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        if (!tenantId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Academic year does not belong to this school");
        }

        if (teacherAssignmentRepository.existsByTeacherIdAndSubjectIdAndClassIdAndAcademicYearId(
                request.getTeacherId(), request.getSubjectId(), request.getClassId(), request.getAcademicYearId())
                && !request.getTeacherId().equals(assignment.getTeacherId())) {
            throw new ConflictException("Teacher assignment already exists");
        }

        assignment.setTeacherId(request.getTeacherId());
        assignment.setSubjectId(request.getSubjectId());
        assignment.setClassId(request.getClassId());
        assignment.setStreamId(request.getStreamId());
        assignment.setAcademicYearId(request.getAcademicYearId());
        assignment.setClassTeacher(request.isClassTeacher());

        teacherAssignmentRepository.save(assignment);

        auditLogService.log("TEACHER_ASSIGNMENT_UPDATE", "TeacherAssignment", assignment.getId(),
                "Updated teacher assignment for teacher " + teacher.getEmail(), null, null, null);

        return mapToDto(assignment, teacher.getFirstName() + " " + teacher.getLastName(), subject.getName(), clazz.getName(), null, academicYear.getName());
    }

    @Transactional
    public void delete(Long id) {
        var assignment = teacherAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(assignment.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        assignment.setDeletedAt(java.time.LocalDateTime.now());
        teacherAssignmentRepository.save(assignment);

        auditLogService.log("TEACHER_ASSIGNMENT_DELETE", "TeacherAssignment", assignment.getId(),
                "Deleted teacher assignment", null, null, null);
    }

    private TeacherAssignmentDto mapToDto(TeacherAssignment entity, String teacherName, String subjectName,
                                          String className, String streamName, String academicYearName) {
        TeacherAssignmentDto dto = new TeacherAssignmentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setTeacherId(entity.getTeacherId());
        dto.setTeacherName(teacherName);
        dto.setSchoolId(entity.getSchoolId());
        dto.setSubjectId(entity.getSubjectId());
        dto.setSubjectName(subjectName);
        dto.setClassId(entity.getClassId());
        dto.setClassName(className);
        dto.setStreamId(entity.getStreamId());
        dto.setStreamName(streamName);
        dto.setAcademicYearId(entity.getAcademicYearId());
        dto.setAcademicYearName(academicYearName);
        dto.setClassTeacher(entity.isClassTeacher());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
