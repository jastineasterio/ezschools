package com.edumanage.streamsubjects.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.security.AccessGuard;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.streamsubjects.dto.SaveStreamSubjectsRequest;
import com.edumanage.streamsubjects.entity.StreamSubjectAssignment;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamSubjectAssignmentServiceTest {

    @Mock private StreamSubjectAssignmentRepository assignmentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private StudentProfileRepository studentRepository;
    @Mock private AcademicYearRepository yearRepository;
    @Mock private ClassRepository classRepository;
    @Mock private StreamRepository streamRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private AccessGuard accessGuard;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private StreamSubjectAssignmentService service;

    private AcademicYear year;
    private Class clazz;
    private Stream stream;
    private Subject math;
    private Subject english;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(7L);
        year = new AcademicYear();
        year.setSchoolId(7L);
        clazz = new Class();
        clazz.setSchoolId(7L);
        stream = new Stream();
        stream.setSchoolId(7L);
        stream.setClassId(30L);
        math = subject(101L, "Mathematics");
        english = subject(102L, "English");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void savePersistsMultipleUniqueSubjectsForOneStream() {
        stubContext();
        stubSubjects();
        when(assignmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.save(request(List.of(101L, 102L, 101L)));

        assertEquals(2, result.size());
        assertEquals(List.of("Mathematics", "English"), result.stream().map(item -> item.getSubjectName()).toList());
        verify(assignmentRepository).deleteBySchoolIdAndClassIdAndStreamIdAndAcademicYearId(7L, 30L, 40L, 20L);
    }

    @Test
    void saveRejectsStreamFromAnotherClass() {
        stubContext();
        stream.setClassId(31L);

        assertThrows(BadRequestException.class, () -> service.save(request(List.of(101L))));

        verify(assignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void saveRejectsSubjectFromAnotherSchool() {
        stubContext();
        when(subjectRepository.findById(101L)).thenReturn(Optional.of(math));
        math.setSchoolId(8L);

        assertThrows(ForbiddenException.class, () -> service.save(request(List.of(101L))));

        verify(assignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void studentSubjectLookupUsesEnrolledStreamAndAcademicYear() {
        when(yearRepository.findById(20L)).thenReturn(Optional.of(year));
        when(subjectRepository.findById(101L)).thenReturn(Optional.of(math));
        StudentProfile student = new StudentProfile();
        student.setSchoolId(7L);
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setSchoolId(7L);
        enrollment.setStudentId(50L);
        enrollment.setAcademicYearId(20L);
        enrollment.setClassId(30L);
        enrollment.setStreamId(40L);
        StreamSubjectAssignment assignment = new StreamSubjectAssignment();
        assignment.setSubjectId(101L);
        assignment.setAcademicYearId(20L);
        assignment.setClassId(30L);
        assignment.setStreamId(40L);
        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(50L, 20L)).thenReturn(Optional.of(enrollment));
        when(assignmentRepository.findBySchoolIdAndClassIdAndStreamIdAndAcademicYearId(7L, 30L, 40L, 20L))
                .thenReturn(List.of(assignment));

        var result = service.subjectsForStudent(50L, 20L);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getSubjectId());
        verify(accessGuard).assertCanAccessStudent(50L);
    }

    @Test
    void emptySelectionClearsExistingStreamSubjects() {
        stubContext();
        when(assignmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.save(request(List.of())).isEmpty());

        verify(assignmentRepository).deleteBySchoolIdAndClassIdAndStreamIdAndAcademicYearId(7L, 30L, 40L, 20L);
    }

    private SaveStreamSubjectsRequest request(List<Long> subjectIds) {
        SaveStreamSubjectsRequest request = new SaveStreamSubjectsRequest();
        request.setAcademicYearId(20L);
        request.setClassId(30L);
        request.setStreamId(40L);
        request.setSubjectIds(subjectIds);
        return request;
    }

    private Subject subject(Long id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setSchoolId(7L);
        subject.setName(name);
        subject.setCode(name.substring(0, 1));
        return subject;
    }

    private void stubContext() {
        when(yearRepository.findById(20L)).thenReturn(Optional.of(year));
        when(classRepository.findById(30L)).thenReturn(Optional.of(clazz));
        when(streamRepository.findById(40L)).thenReturn(Optional.of(stream));
    }

    private void stubSubjects() {
        when(subjectRepository.findById(101L)).thenReturn(Optional.of(math));
        when(subjectRepository.findById(102L)).thenReturn(Optional.of(english));
    }
}
