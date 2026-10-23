package com.edumanage.teacherassignments.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.roles.entity.Role;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.teacherassignments.dto.CreateTeacherAssignmentRequest;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        if (subjectConfigured) {
            when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }
    }
}
