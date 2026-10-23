package com.edumanage.teacherassignments.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.teacherassignments.dto.CreateTeacherAssignmentRequest;
import com.edumanage.teacherassignments.dto.CreateTeacherAssignmentBatchRequest;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
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
    private final TeacherProfileRepository teacherProfileRepository;
    private final StreamSubjectAssignmentRepository streamSubjectAssignmentRepository;

    public TeacherAssignmentService(TeacherAssignmentRepository teacherAssignmentRepository, UserRepository userRepository,
                                    SubjectRepository subjectRepository, ClassRepository classRepository,
                                    StreamRepository streamRepository, AcademicYearRepository academicYearRepository,
                                    AuditLogService auditLogService,
                                    TeacherProfileRepository teacherProfileRepository,
                                    StreamSubjectAssignmentRepository streamSubjectAssignmentRepository) {
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.academicYearRepository = academicYearRepository;
        this.auditLogService = auditLogService;
        this.teacherProfileRepository = teacherProfileRepository;
        this.streamSubjectAssignmentRepository = streamSubjectAssignmentRepository;
    }

    public PageResponse<TeacherAssignmentDto> list(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<TeacherAssignment> page = teacherAssignmentRepository.findBySchoolIdAndDeletedAtIsNull(tenantId, pageable);
        return PageResponse.from(page.map(assignment -> mapToDto(assignment,
                assignment.getTeacher().getFirstName() + " " + assignment.getTeacher().getLastName(),
                assignment.getSubject().getName(),
                assignment.getClazz().getName(),
                assignment.getStream() == null ? null : assignment.getStream().getName(),
                assignment.getAcademicYear().getName())));
    }

    public List<TeacherAssignmentDto> listByTeacher(Long teacherId) {
        Long tenantId = TenantContext.requireTenantId();
        var teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (!tenantId.equals(teacher.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return teacherAssignmentRepository.findByTeacherIdAndSchoolIdAndDeletedAtIsNull(teacherId, tenantId).stream()
                .map(assignment -> mapToDto(assignment,
                        assignment.getTeacher().getFirstName() + " " + assignment.getTeacher().getLastName(),
                        assignment.getSubject().getName(),
                        assignment.getClazz().getName(),
                        assignment.getStream() == null ? null : assignment.getStream().getName(),
                        assignment.getAcademicYear().getName()))
                .collect(Collectors.toList());
    }

    public TeacherAssignmentDto getById(Long id) {
        var assignment = teacherAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment not found"));

        TenantContext.assertTenantAccess(assignment.getSchoolId());

        return mapToDto(assignment, null, null, null, null, null);
    }

    @Transactional
    public TeacherAssignmentDto create(CreateTeacherAssignmentRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        var refs = validateRequest(tenantId, request);
        if (teacherAssignmentRepository.existsByTeacherIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
                request.getTeacherId(), request.getSubjectId(), request.getClassId(), request.getStreamId(),
                request.getAcademicYearId())) {
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
                "Created teacher assignment for teacher " + refs.teacher().getEmail(), null, null, null);

        return mapToDto(assignment, refs.teacher().getFirstName() + " " + refs.teacher().getLastName(),
                refs.subject().getName(), refs.clazz().getName(), refs.streamName(), refs.academicYear().getName());
    }

    @Transactional
    public List<TeacherAssignmentDto> createBatch(CreateTeacherAssignmentBatchRequest request) {
        return request.getSubjectIds().stream().distinct().map(subjectId -> {
            CreateTeacherAssignmentRequest assignment = new CreateTeacherAssignmentRequest();
            assignment.setTeacherId(request.getTeacherId());
            assignment.setSubjectId(subjectId);
            assignment.setClassId(request.getClassId());
            assignment.setStreamId(request.getStreamId());
            assignment.setAcademicYearId(request.getAcademicYearId());
            assignment.setClassTeacher(request.isClassTeacher());
            return create(assignment);
        }).toList();
    }

    @Transactional
    public TeacherAssignmentDto update(Long id, CreateTeacherAssignmentRequest request) {
        var assignment = teacherAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment not found"));

        Long tenantId = TenantContext.requireTenantId();
        TenantContext.assertTenantAccess(assignment.getSchoolId());
        var refs = validateRequest(tenantId, request);
        if (teacherAssignmentRepository.existsByTeacherIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndIdNotAndDeletedAtIsNull(
                request.getTeacherId(), request.getSubjectId(), request.getClassId(), request.getStreamId(),
                request.getAcademicYearId(), assignment.getId())) {
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
                "Updated teacher assignment for teacher " + refs.teacher().getEmail(), null, null, null);

        return mapToDto(assignment, refs.teacher().getFirstName() + " " + refs.teacher().getLastName(),
                refs.subject().getName(), refs.clazz().getName(), refs.streamName(), refs.academicYear().getName());
    }

    @Transactional
    public void delete(Long id) {
        var assignment = teacherAssignmentRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment not found"));

        TenantContext.assertTenantAccess(assignment.getSchoolId());

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
        if (teacherName == null) {
            var teacher = userRepository.findById(entity.getTeacherId()).orElse(null);
            teacherName = teacher == null ? null : teacher.getFirstName() + " " + teacher.getLastName();
        }
        dto.setTeacherName(teacherName);
        dto.setSchoolId(entity.getSchoolId());
        dto.setSubjectId(entity.getSubjectId());
        if (subjectName == null) {
            subjectName = subjectRepository.findById(entity.getSubjectId()).map(value -> value.getName()).orElse(null);
        }
        dto.setSubjectName(subjectName);
        dto.setClassId(entity.getClassId());
        if (className == null) {
            className = classRepository.findById(entity.getClassId()).map(value -> value.getName()).orElse(null);
        }
        dto.setClassName(className);
        dto.setStreamId(entity.getStreamId());
        if (streamName == null && entity.getStreamId() != null) {
            streamName = streamRepository.findById(entity.getStreamId()).map(value -> value.getName()).orElse(null);
        }
        dto.setStreamName(streamName);
        dto.setAcademicYearId(entity.getAcademicYearId());
        if (academicYearName == null) {
            academicYearName = academicYearRepository.findById(entity.getAcademicYearId())
                    .map(value -> value.getName()).orElse(null);
        }
        dto.setAcademicYearName(academicYearName);
        dto.setClassTeacher(entity.isClassTeacher());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private AssignmentReferences validateRequest(Long schoolId, CreateTeacherAssignmentRequest request) {
        var teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        boolean hasTeacherRole = teacher.getRoles().stream()
                .anyMatch(role -> "TEACHER".equals(role.getName()));
        if (!hasTeacherRole || !schoolId.equals(teacher.getSchoolId())) {
            throw new ForbiddenException("Teacher is not eligible in this school");
        }
        var profile = teacherProfileRepository.findBySchoolIdAndUserId(schoolId, request.getTeacherId())
                .filter(value -> !value.isDeleted() && "ACTIVE".equals(value.getEmploymentStatus()))
                .orElseThrow(() -> new ForbiddenException("An active teacher profile is required"));

        var subject = subjectRepository.findById(request.getSubjectId())
                .filter(value -> !value.isDeleted() && "ACTIVE".equals(value.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!schoolId.equals(subject.getSchoolId())) {
            throw new ForbiddenException("Subject does not belong to this school");
        }
        var clazz = classRepository.findById(request.getClassId())
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!schoolId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Class does not belong to this school");
        }
        String streamName = null;
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
            streamName = stream.getName();
        }
        var academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        if (!schoolId.equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Academic year does not belong to this school");
        }
        if (request.getStreamId() != null
                && !streamSubjectAssignmentRepository.existsBySchoolIdAndClassIdAndStreamIdAndAcademicYearIdAndSubjectId(
                schoolId, request.getClassId(), request.getStreamId(), request.getAcademicYearId(),
                request.getSubjectId())) {
            throw new BadRequestException("Subject is not configured for the selected class stream");
        }
        return new AssignmentReferences(teacher, subject, clazz, streamName, academicYear);
    }

    private record AssignmentReferences(com.edumanage.users.entity.User teacher,
                                        com.edumanage.subjects.entity.Subject subject,
                                        com.edumanage.classes.entity.Class clazz,
                                        String streamName,
                                        com.edumanage.academicyears.entity.AcademicYear academicYear) {}
}
