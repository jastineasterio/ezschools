package com.edumanage.streamsubjects.service;

import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.security.AccessGuard;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.streamsubjects.dto.SaveStreamSubjectsRequest;
import com.edumanage.streamsubjects.dto.StreamSubjectAssignmentDto;
import com.edumanage.streamsubjects.entity.StreamSubjectAssignment;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StreamSubjectAssignmentService {

    private final StreamSubjectAssignmentRepository assignmentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final SubjectRepository subjectRepository;
    private final AccessGuard accessGuard;
    private final AuditLogService auditLogService;

    public StreamSubjectAssignmentService(StreamSubjectAssignmentRepository assignmentRepository,
                                          StudentEnrollmentRepository enrollmentRepository,
                                          StudentProfileRepository studentRepository,
                                          AcademicYearRepository academicYearRepository,
                                          ClassRepository classRepository,
                                          StreamRepository streamRepository,
                                          SubjectRepository subjectRepository,
                                          AccessGuard accessGuard,
                                          AuditLogService auditLogService) {
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.academicYearRepository = academicYearRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.subjectRepository = subjectRepository;
        this.accessGuard = accessGuard;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<StreamSubjectAssignmentDto> list(Long academicYearId, Long classId, Long streamId) {
        Long schoolId = TenantContext.requireTenantId();
        validateContext(schoolId, academicYearId, classId, streamId);
        return assignmentRepository
                .findBySchoolIdAndClassIdAndStreamIdAndAcademicYearId(schoolId, classId, streamId, academicYearId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<StreamSubjectAssignmentDto> save(SaveStreamSubjectsRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        validateContext(schoolId, request.getAcademicYearId(), request.getClassId(), request.getStreamId());

        List<Long> subjectIds = request.getSubjectIds().stream().distinct().toList();
        List<Subject> subjects = subjectIds.stream()
                .map(id -> subjectRepository.findById(id)
                        .filter(subject -> !subject.isDeleted() && "ACTIVE".equals(subject.getStatus()))
                        .orElseThrow(() -> new ResourceNotFoundException("Subject not found")))
                .toList();
        if (subjects.stream().anyMatch(subject -> !schoolId.equals(subject.getSchoolId()))) {
            throw new ForbiddenException("Subject does not belong to this school");
        }

        assignmentRepository.deleteBySchoolIdAndClassIdAndStreamIdAndAcademicYearId(
                schoolId, request.getClassId(), request.getStreamId(), request.getAcademicYearId());

        List<StreamSubjectAssignment> assignments = subjects.stream().map(subject -> {
            StreamSubjectAssignment assignment = new StreamSubjectAssignment();
            assignment.setSchoolId(schoolId);
            assignment.setClassId(request.getClassId());
            assignment.setStreamId(request.getStreamId());
            assignment.setAcademicYearId(request.getAcademicYearId());
            assignment.setSubjectId(subject.getId());
            return assignment;
        }).toList();

        List<StreamSubjectAssignmentDto> saved = assignmentRepository.saveAll(assignments).stream()
                .map(this::toDto)
                .toList();
        auditLogService.log("STREAM_SUBJECTS_UPDATE", "StreamSubjectAssignment", request.getStreamId(),
                "Updated subjects for class stream and academic year", null, null, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StreamSubjectAssignmentDto> subjectsForStudent(Long studentId, Long academicYearId) {
        accessGuard.assertCanAccessStudent(studentId);
        Long schoolId = TenantContext.getTenantId();
        var student = studentRepository.findById(studentId)
                .filter(profile -> !profile.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        TenantContext.assertTenantAccess(student.getSchoolId());

        var enrollment = enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .filter(record -> !record.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student enrollment not found"));
        if (!student.getSchoolId().equals(enrollment.getSchoolId())
                || (schoolId != null && !schoolId.equals(enrollment.getSchoolId()))) {
            throw new ForbiddenException("Access denied");
        }
        var academicYear = academicYearRepository.findById(academicYearId)
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        if (!enrollment.getSchoolId().equals(academicYear.getSchoolId())) {
            throw new ForbiddenException("Academic year does not belong to this school");
        }
        if (!"ENROLLED".equals(enrollment.getStatus()) || enrollment.getStreamId() == null) {
            return List.of();
        }

        return assignmentRepository.findBySchoolIdAndClassIdAndStreamIdAndAcademicYearId(
                        enrollment.getSchoolId(), enrollment.getClassId(), enrollment.getStreamId(), academicYearId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void validateContext(Long schoolId, Long academicYearId, Long classId, Long streamId) {
        var year = academicYearRepository.findById(academicYearId)
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        var clazz = classRepository.findById(classId)
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        var stream = streamRepository.findById(streamId)
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
        if (!schoolId.equals(year.getSchoolId())
                || !schoolId.equals(clazz.getSchoolId())
                || !schoolId.equals(stream.getSchoolId())) {
            throw new ForbiddenException("Academic year, class, or stream does not belong to this school");
        }
        if (!classId.equals(stream.getClassId())) {
            throw new BadRequestException("Stream does not belong to the selected class");
        }
    }

    private StreamSubjectAssignmentDto toDto(StreamSubjectAssignment assignment) {
        Subject subject = subjectRepository.findById(assignment.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        return new StreamSubjectAssignmentDto(assignment.getId(), subject.getId(), subject.getName(),
                subject.getCode(), assignment.getAcademicYearId(), assignment.getClassId(), assignment.getStreamId());
    }
}
