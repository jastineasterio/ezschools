package com.edumanage.teacherworkspace.service;

import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.exception.UnauthorizedException;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.notifications.repository.NotificationRepository;
import com.edumanage.security.CurrentUserService;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teacherassignments.service.TeacherAssignmentService;
import com.edumanage.teachertimetables.repository.TeacherTimetableRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherWorkspaceServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private TeacherAssignmentService assignmentService;
    @Mock private SchoolRepository schoolRepository;
    @Mock private ExaminationSubjectRepository examinationSubjectRepository;
    @Mock private TeacherTimetableRepository timetableRepository;
    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private TeacherWorkspaceService service;

    private User teacherWithSchool;
    private User teacherWithoutSchool;
    private School school;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        teacherWithSchool = new User();
        teacherWithSchool.setId(1L);
        teacherWithSchool.setEmail("teacher@school.test");
        teacherWithSchool.setSchoolId(7L);
        teacherWithSchool.setRoles(Set.of(teacherRole()));

        teacherWithoutSchool = new User();
        teacherWithoutSchool.setId(2L);
        teacherWithoutSchool.setEmail("noschool@school.test");
        teacherWithoutSchool.setSchoolId(null);
        teacherWithoutSchool.setRoles(Set.of(teacherRole()));

        school = new School();
        school.setId(7L);
        school.setName("Demo School");
        school.setTimeZone("UTC");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void dashboardLoadsForAuthenticatedTeacherWithSchool() {
        when(currentUserService.requireCurrentUser()).thenReturn(teacherWithSchool);
        when(currentUserService.hasRole(teacherWithSchool, "TEACHER")).thenReturn(true);
        when(schoolRepository.findById(7L)).thenReturn(java.util.Optional.of(school));
        when(assignmentService.listByTeacher(1L)).thenReturn(List.of());
        when(examinationSubjectRepository.findUpcomingForTeacher(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(List.of());
        when(notificationRepository.countByUserIdAndSchoolIdAndIsReadFalse(1L, 7L)).thenReturn(0L);

        var dto = service.dashboard();

        assertNotNull(dto);
        assertNotNull(dto.getToday());
        assertEquals("UTC", dto.getTimeZone());
        verify(currentUserService).requireCurrentUser();
        verify(assignmentService).listByTeacher(1L);
    }

    @Test
    void assignmentsLoadForAuthenticatedTeacherWithSchool() {
        when(currentUserService.requireCurrentUser()).thenReturn(teacherWithSchool);
        when(currentUserService.hasRole(teacherWithSchool, "TEACHER")).thenReturn(true);
        when(assignmentService.listByTeacher(1L)).thenReturn(List.of());

        List<TeacherAssignmentDto> items = service.assignments();

        assertNotNull(items);
        assertTrue(items.isEmpty());
        verify(assignmentService).listByTeacher(1L);
    }

    @Test
    void requireSchoolDoesNotRecurse() {
        when(currentUserService.requireCurrentUser()).thenReturn(teacherWithSchool);
        when(currentUserService.hasRole(teacherWithSchool, "TEACHER")).thenReturn(true);
        when(assignmentService.listByTeacher(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.assignments());
    }

    @Test
    void dashboardRejectsTeacherWithoutSchoolAssociation() {
        when(currentUserService.requireCurrentUser()).thenReturn(teacherWithoutSchool);
        when(currentUserService.hasRole(teacherWithoutSchool, "TEACHER")).thenReturn(true);

        assertThrows(ResourceNotFoundException.class, () -> service.dashboard());
    }

    @Test
    void assignmentsRejectsTeacherWithoutSchoolAssociation() {
        when(currentUserService.requireCurrentUser()).thenReturn(teacherWithoutSchool);
        when(currentUserService.hasRole(teacherWithoutSchool, "TEACHER")).thenReturn(true);

        assertThrows(ResourceNotFoundException.class, () -> service.assignments());
    }

    @Test
    void dashboardRejectsUnauthenticatedRequest() {
        when(currentUserService.requireCurrentUser())
                .thenThrow(new UnauthorizedException("Not authenticated"));

        assertThrows(UnauthorizedException.class, () -> service.dashboard());
    }

    @Test
    void assignmentsRejectsUnauthenticatedRequest() {
        when(currentUserService.requireCurrentUser())
                .thenThrow(new UnauthorizedException("Not authenticated"));

        assertThrows(UnauthorizedException.class, () -> service.assignments());
    }

    private static com.edumanage.roles.entity.Role teacherRole() {
        com.edumanage.roles.entity.Role role = new com.edumanage.roles.entity.Role();
        role.setName("TEACHER");
        return role;
    }
}
