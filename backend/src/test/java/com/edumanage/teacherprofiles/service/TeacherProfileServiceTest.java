package com.edumanage.teacherprofiles.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.exception.UnauthorizedException;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.teacherprofiles.dto.CreateTeacherProfileRequest;
import com.edumanage.teacherprofiles.dto.TeacherProfileDto;
import com.edumanage.teacherprofiles.dto.UpdateTeacherProfileRequest;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.tenant.TenantContext;
import com.edumanage.roles.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherProfileServiceTest {

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TeacherProfileService service;

    private static final Long USER_ID = 1L;
    private static final Long SCHOOL_ID = 10L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(SCHOOL_ID);
        CustomUserDetails principal = new CustomUserDetails(
                USER_ID,
                "teacher@school.com",
                "hash",
                SCHOOL_ID,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private TeacherProfile profile() {
        TeacherProfile p = new TeacherProfile();
        p.setId(100L);
        p.setUserId(USER_ID);
        p.setSchoolId(SCHOOL_ID);
        p.setEmployeeNumber("EMP-001");
        p.setGender("MALE");
        p.setDateOfBirth(LocalDate.of(1985, 3, 10));
        p.setEmploymentStatus("ACTIVE");
        p.setQualification("B.Ed");
        p.setExperienceYears(5);
        p.setPhotoUrl("https://example.com/photo.jpg");
        p.setCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        p.setUpdatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        return p;
    }

    private User user() {
        User u = new User();
        u.setId(USER_ID);
        u.setEmail("teacher@school.com");
        u.setFirstName("Jastine");
        u.setLastName("John");
        u.setPhone("+255712345678");
        u.setSchoolId(SCHOOL_ID);
        u.setActive(true);
        u.setLastLogin(LocalDateTime.of(2024, 1, 1, 0, 0));
        u.setCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        u.setUpdatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        Role teacherRole = new Role();
        teacherRole.setName("TEACHER");
        u.getRoles().add(teacherRole);
        return u;
    }

    private School school() {
        School s = new School();
        s.setId(SCHOOL_ID);
        s.setName("Springfield High School");
        s.setRegistrationNumber("SH-100");
        return s;
    }

    @Test
    void teacherCanRetrieveCompleteProfile() {
        when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school()));

        TeacherProfileDto dto = service.getMyProfile();

        assertNotNull(dto);
        assertEquals("TEACHER", dto.getRole());
        assertEquals("Springfield High School", dto.getSchoolName());
        assertEquals("SH-100", dto.getSchoolCode());
        assertEquals("Jastine", dto.getFirstName());
        assertEquals("John", dto.getLastName());
        assertEquals("teacher@school.com", dto.getEmail());
        assertEquals("+255712345678", dto.getPhone());
        assertEquals("MALE", dto.getGender());
        assertEquals("ACTIVE", dto.getEmploymentStatus());
        assertTrue(dto.isActive());
        assertEquals("EMP-001", dto.getEmployeeNumber());
    }

    @Test
    void teacherCanUpdatePermittedPersonalInformation() {
        User currentUser = user();
        currentUser.setEmail("teacher@school.com");
        when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(currentUser));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school()));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teacherProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("teacher@school.com");
        request.setPhone("+255798765432");
        request.setGender("FEMALE");
        request.setDateOfBirth(LocalDate.of(1990, 5, 20));
        request.setPhotoUrl("https://example.com/new.jpg");

        TeacherProfileDto dto = service.updateMyProfile(request);

        assertNotNull(dto);
        assertEquals("Jane", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("+255798765432", dto.getPhone());
        assertEquals("FEMALE", dto.getGender());

        verify(userRepository).save(currentUser);
        verify(teacherProfileRepository).save(any());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userRepository.existsByEmailIgnoreCase("other@school.com")).thenReturn(true);

        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setFirstName("Jastine");
        request.setLastName("John");
        request.setEmail("other@school.com");

        assertThrows(ConflictException.class, () -> service.updateMyProfile(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void schoolAndRoleRemainReadOnlyOnUpdate() {
        User currentUser = user();
        when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(currentUser));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school()));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teacherProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setEmail("teacher@school.com");

        service.updateMyProfile(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(SCHOOL_ID, saved.getSchoolId());
        assertTrue(saved.getRoles().stream().anyMatch(r -> "TEACHER".equals(r.getName())));
        assertEquals(1, saved.getRoles().size());

        ArgumentCaptor<TeacherProfile> profileCaptor = ArgumentCaptor.forClass(TeacherProfile.class);
        verify(teacherProfileRepository).save(profileCaptor.capture());
        assertEquals(SCHOOL_ID, profileCaptor.getValue().getSchoolId());
    }

    @Test
    void teacherOnlyAccessesOwnProfile() {
        when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school()));

        service.getMyProfile();

        verify(teacherProfileRepository).findByUserId(USER_ID);
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, () -> service.getMyProfile());
    }

    @Test
    void teacherCannotAccessOtherSchoolProfile() {
        TeacherProfile otherSchoolProfile = profile();
        otherSchoolProfile.setSchoolId(99L);
        when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(otherSchoolProfile));

        assertThrows(ForbiddenException.class, () -> service.getMyProfile());
    }

    @Test
    void listUsesTenantIsolation() {
        Page<TeacherProfile> emptyPage = org.springframework.data.domain.Page.empty();
        when(teacherProfileRepository.findBySchoolId(org.mockito.Mockito.eq(SCHOOL_ID), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<TeacherProfileDto> page = service.list(Pageable.unpaged());

        assertNotNull(page);
        verify(teacherProfileRepository).findBySchoolId(org.mockito.Mockito.eq(SCHOOL_ID), any(Pageable.class));
    }

    @Test
    void createSetsTeacherRoleAndSchool() {
        User teacherUser = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(teacherUser));
        when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school()));
        when(teacherProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateTeacherProfileRequest request = new CreateTeacherProfileRequest();
        request.setUserId(USER_ID);
        request.setEmployeeNumber("EMP-002");
        request.setGender("FEMALE");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setEmploymentStatus("ACTIVE");
        request.setQualification("M.Ed");
        request.setExperienceYears(3);
        request.setPhotoUrl(null);

        service.create(request);

        ArgumentCaptor<TeacherProfile> captor = ArgumentCaptor.forClass(TeacherProfile.class);
        verify(teacherProfileRepository).save(captor.capture());
        TeacherProfile saved = captor.getValue();
        assertEquals(SCHOOL_ID, saved.getSchoolId());
        assertEquals(USER_ID, saved.getUserId());
    }
}
