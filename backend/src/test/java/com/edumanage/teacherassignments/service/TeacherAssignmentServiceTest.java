package com.edumanage.teacherassignments.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.roles.entity.Role;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.teacherassignments.dto.CreateTeacherAssignmentRequest;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherAssignmentServiceTest {

    @Mock private TeacherAssignmentRepository assignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ClassRepository classRepository;
    @Mock private StreamRepository streamRepository;
    @Mock private AcademicYearRepository yearRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private TeacherProfileRepository teacherProfileRepository;
    @Mock private StreamSubjectAssignmentRepository streamSubjectRepository;
    @InjectMocks private TeacherAssignmentService service;

    private Role teacherRole;
    private Subject subject;
    private Class clazz;
    private Stream stream;
    private AcademicYear year;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(7L);
        teacherRole = new Role();
        teacherRole.setName("TEACHER");
        subject = new Subject();
        subject.setSchoolId(7L);
        subject.setName("Mathematics");
        clazz = new Class();
        clazz.setSchoolId(7L);
        clazz.setName("Form One");
        stream = new Stream();
        stream.setSchoolId(7L);
        stream.setClassId(30L);
        stream.setName("A");
        year = new AcademicYear();
        year.setSchoolId(7L);
        year.setName("2026");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void differentTeachersMayTeachSameSubjectInSameClassStream() {
        stubContext(true);
        stubEligibleTeacher(1L);
        stubEligibleTeacher(2L);
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request(1L, 100L));
        service.create(request(2L, 100L));

        ArgumentCaptor<TeacherAssignment> captor = ArgumentCaptor.forClass(TeacherAssignment.class);
        verify(assignmentRepository, times(2)).save(captor.capture());
        assertEquals(List.of(1L, 2L), captor.getAllValues().stream().map(TeacherAssignment::getTeacherId).toList());
    }

    @Test
    void rejectsTeacherWithoutAnActiveSchoolProfile() {
        stubEligibleUser(1L);
        when(teacherProfileRepository.findBySchoolIdAndUserId(7L, 1L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.create(request(1L, 100L)));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void rejectsStreamSubjectThatIsNotConfiguredForTheClassStream() {
        stubContext(false);
        stubEligibleTeacher(1L);

        assertThrows(BadRequestException.class, () -> service.create(request(1L, 100L)));

        verify(assignmentRepository, never()).save(any());
    }

    private CreateTeacherAssignmentRequest request(Long teacherId, Long subjectId) {
        CreateTeacherAssignmentRequest request = new CreateTeacherAssignmentRequest();
        request.setTeacherId(teacherId);
        request.setSubjectId(subjectId);
        request.setClassId(30L);
        request.setStreamId(40L);
        request.setAcademicYearId(20L);
        return request;
    }

    private void stubEligibleTeacher(Long teacherId) {
        stubEligibleUser(teacherId);
        TeacherProfile profile = new TeacherProfile();
        profile.setSchoolId(7L);
        profile.setEmploymentStatus("ACTIVE");
        when(teacherProfileRepository.findBySchoolIdAndUserId(7L, teacherId)).thenReturn(Optional.of(profile));
    }

    private void stubEligibleUser(Long teacherId) {
        User teacher = new User();
        teacher.setSchoolId(7L);
        teacher.setEmail("teacher" + teacherId + "@school.test");
        teacher.setFirstName("Teacher");
        teacher.setLastName(String.valueOf(teacherId));
        teacher.setRoles(Set.of(teacherRole));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    }

    private void stubContext(boolean subjectConfigured) {
        when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));
        when(classRepository.findById(30L)).thenReturn(Optional.of(clazz));
        when(streamRepository.findById(40L)).thenReturn(Optional.of(stream));
        when(yearRepository.findById(20L)).thenReturn(Optional.of(year));
        when(streamSubjectRepository.existsBySchoolIdAndClassIdAndStreamIdAndAcademicYearIdAndSubjectId(
                7L, 30L, 40L, 20L, 100L)).thenReturn(subjectConfigured);
    }

    @Test
    void createsAssignmentWhenNoExistingAssignmentExists() {
        stubContext(true);
        stubEligibleTeacher(1L);
        when(assignmentRepository.findByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearId(
                1L, 7L, 100L, 30L, 40L, 20L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherAssignmentDto dto = service.create(request(1L, 100L));
        assertEquals(1L, dto.getTeacherId());
        assertEquals(100L, dto.getSubjectId());
        assertEquals(30L, dto.getClassId());
        assertEquals(40L, dto.getStreamId());
        assertEquals(20L, dto.getAcademicYearId());
        ArgumentCaptor<TeacherAssignment> captor = ArgumentCaptor.forClass(TeacherAssignment.class);
        verify(assignmentRepository, times(1)).save(captor.capture());
        assertNull(captor.getValue().getDeletedAt());
        verify(auditLogService, times(1)).log(eq("TEACHER_ASSIGNMENT_CREATE"), any(), any(), any(), any(), any(),
                any());
    }

    @Test
    void doesNotInsertDuplicateWhenActiveAssignmentAlreadyExists() {
        stubContext(true);
        stubEligibleTeacher(1L);
        TeacherAssignment active = new TeacherAssignment();
        active.setId(100L);
        active.setTeacherId(1L);
        active.setSchoolId(7L);
        active.setSubjectId(100L);
        active.setClassId(30L);
        active.setStreamId(40L);
        active.setAcademicYearId(20L);
        when(assignmentRepository.findByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearId(
                1L, 7L, 100L, 30L, 40L, 20L)).thenReturn(Optional.of(active));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request(1L, 100L)));
        assertTrue(ex.getMessage().contains("already assigned"));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void reactivatesSoftDeletedAssignmentInsteadOfDuplicating() {
        stubContext(true);
        stubEligibleTeacher(1L);
        TeacherAssignment deleted = new TeacherAssignment();
        deleted.setId(100L);
        deleted.setDeletedAt(LocalDateTime.now());
        deleted.setTeacherId(1L);
        deleted.setSchoolId(7L);
        deleted.setSubjectId(100L);
        deleted.setClassId(30L);
        deleted.setStreamId(40L);
        deleted.setAcademicYearId(20L);
        when(assignmentRepository.findByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearId(
                1L, 7L, 100L, 30L, 40L, 20L)).thenReturn(Optional.of(deleted));
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherAssignmentDto dto = service.create(request(1L, 100L));

        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getTeacherId());
        ArgumentCaptor<TeacherAssignment> captor = ArgumentCaptor.forClass(TeacherAssignment.class);
        verify(assignmentRepository, times(1)).save(captor.capture());
        assertNull(captor.getValue().getDeletedAt());
        assertEquals(100L, captor.getValue().getId());
        verify(auditLogService, times(1)).log(eq("TEACHER_ASSIGNMENT_REACTIVATE"), any(), any(), any(), any(), any(),
                any());
    }

    @Test
    void concurrentDuplicateRequestIsTranslatedToConflictNotServerError() {
        stubContext(true);
        stubEligibleTeacher(1L);
        when(assignmentRepository.findByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearId(
                1L, 7L, 100L, 30L, 40L, 20L)).thenReturn(Optional.empty());
        AtomicInteger saves = new AtomicInteger();
        when(assignmentRepository.save(any())).thenAnswer(invocation -> {
            if (saves.incrementAndGet() == 2) {
                throw new DataIntegrityViolationException("duplicate key violates unique constraint");
            }
            return invocation.getArgument(0);
        });

        service.create(request(1L, 100L));
        assertThrows(ConflictException.class, () -> service.create(request(1L, 100L)));

        assertEquals(2, saves.get());
        verify(assignmentRepository, times(2)).save(any());
    }

    @Test
    void listReturnsExistingAssignmentsForAuthenticatedSchool() {
        User teacher = new User();
        teacher.setFirstName("Jane");
        teacher.setLastName("Doe");
        Subject subject = new Subject();
        subject.setName("Mathematics");
        Class clazz = new Class();
        clazz.setName("Form One");
        Stream stream = new Stream();
        stream.setName("A");
        AcademicYear year = new AcademicYear();
        year.setName("2026");

        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setId(1L);
        assignment.setSchoolId(7L);
        assignment.setTeacherId(1L);
        assignment.setSubjectId(100L);
        assignment.setClassId(30L);
        assignment.setStreamId(40L);
        assignment.setAcademicYearId(20L);
        assignment.setTeacher(teacher);
        assignment.setSubject(subject);
        assignment.setClazz(clazz);
        assignment.setStream(stream);
        assignment.setAcademicYear(year);

        Page<TeacherAssignment> page = new PageImpl<>(List.of(assignment), PageRequest.of(0, 500), 1);
        when(assignmentRepository.findBySchoolIdAndDeletedAtIsNull(eq(7L), any(Pageable.class))).thenReturn(page);

        PageResponse<TeacherAssignmentDto> result = service.list(PageRequest.of(0, 500));

        assertEquals(1, result.getContent().size());
        assertEquals("Jane Doe", result.getContent().get(0).getTeacherName());
        assertEquals("Mathematics", result.getContent().get(0).getSubjectName());
        assertEquals("Form One", result.getContent().get(0).getClassName());
        assertEquals("A", result.getContent().get(0).getStreamName());
        assertEquals("2026", result.getContent().get(0).getAcademicYearName());
        verify(assignmentRepository).findBySchoolIdAndDeletedAtIsNull(eq(7L), any(Pageable.class));
        verify(assignmentRepository, never()).findBySchoolIdAndDeletedAtIsNull(eq(99L), any(Pageable.class));
    }

    @Test
    void differentAcademicYearsAreTreatedAsSeparateAssignments() {
        stubContext(true);
        stubEligibleTeacher(1L);
        when(assignmentRepository.findByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearId(
                1L, 7L, 100L, 30L, 40L, 20L)).thenReturn(Optional.empty());
        when(assignmentRepository.findByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearId(
                1L, 7L, 100L, 30L, 40L, 21L)).thenReturn(Optional.empty());
        when(yearRepository.findById(21L)).thenReturn(Optional.of(year));
        when(streamSubjectRepository.existsBySchoolIdAndClassIdAndStreamIdAndAcademicYearIdAndSubjectId(
                7L, 30L, 40L, 21L, 100L)).thenReturn(true);
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request(1L, 100L, 20L));
        service.create(request(1L, 100L, 21L));

        verify(assignmentRepository, times(2)).save(any());
    }

    private CreateTeacherAssignmentRequest request(Long teacherId, Long subjectId, Long academicYearId) {
        CreateTeacherAssignmentRequest request = new CreateTeacherAssignmentRequest();
        request.setTeacherId(teacherId);
        request.setSubjectId(subjectId);
        request.setClassId(30L);
        request.setStreamId(40L);
        request.setAcademicYearId(academicYearId);
        return request;
    }
}
