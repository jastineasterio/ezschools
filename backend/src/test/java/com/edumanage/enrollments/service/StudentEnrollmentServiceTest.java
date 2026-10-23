package com.edumanage.enrollments.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.enrollments.dto.CreateStudentEnrollmentRequest;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.security.AccessGuard;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentEnrollmentServiceTest {

    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private StudentProfileRepository studentRepository;
    @Mock private AcademicYearRepository yearRepository;
    @Mock private ClassRepository classRepository;
    @Mock private StreamRepository streamRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private AccessGuard accessGuard;
    @InjectMocks private StudentEnrollmentService service;

    private StudentProfile student;
    private AcademicYear year;
    private Class clazz;
    private Stream stream;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(7L);
        student = new StudentProfile();
        student.setSchoolId(7L);
        student.setFirstName("Amina");
        student.setLastName("Njeri");
        year = new AcademicYear();
        year.setSchoolId(7L);
        year.setName("2026");
        clazz = new Class();
        clazz.setSchoolId(7L);
        clazz.setName("Form One");
        stream = new Stream();
        stream.setSchoolId(7L);
        stream.setClassId(30L);
        stream.setName("A");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createPersistsEnrollmentForSchoolStudentClassAndStream() {
        stubBaseContext();
        when(streamRepository.findById(40L)).thenReturn(Optional.of(stream));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(10L, 20L)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request(30L, 40L));

        ArgumentCaptor<StudentEnrollment> captor = ArgumentCaptor.forClass(StudentEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getSchoolId());
        assertEquals(30L, captor.getValue().getClassId());
        assertEquals(40L, captor.getValue().getStreamId());
        assertEquals("Amina Njeri", result.getStudentName());
    }

    @Test
    void createRejectsStudentFromAnotherSchool() {
        student.setSchoolId(8L);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));

        assertThrows(ForbiddenException.class, () -> service.create(request(30L, 40L)));

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void createRejectsClassFromAnotherSchool() {
        stubBaseContext();
        clazz.setSchoolId(8L);

        assertThrows(ForbiddenException.class, () -> service.create(request(30L, 40L)));

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void createRejectsStreamBelongingToDifferentClass() {
        stubBaseContext();
        when(streamRepository.findById(40L)).thenReturn(Optional.of(stream));
        stream.setClassId(31L);

        assertThrows(BadRequestException.class, () -> service.create(request(30L, 40L)));

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEnrollmentForAcademicYear() {
        stubBaseContext();
        when(streamRepository.findById(40L)).thenReturn(Optional.of(stream));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(10L, 20L))
                .thenReturn(Optional.of(new StudentEnrollment()));

        assertThrows(ConflictException.class, () -> service.create(request(30L, 40L)));

        verify(enrollmentRepository, never()).save(any());
    }

    private CreateStudentEnrollmentRequest request(Long classId, Long streamId) {
        CreateStudentEnrollmentRequest request = new CreateStudentEnrollmentRequest();
        request.setStudentId(10L);
        request.setAcademicYearId(20L);
        request.setClassId(classId);
        request.setStreamId(streamId);
        request.setStartDate(LocalDate.of(2026, 1, 1));
        return request;
    }

    private void stubBaseContext() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(yearRepository.findById(20L)).thenReturn(Optional.of(year));
        when(classRepository.findById(30L)).thenReturn(Optional.of(clazz));
    }
}
