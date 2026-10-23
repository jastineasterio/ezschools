package com.edumanage.users.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.parents.repository.ParentRepository;
import com.edumanage.roles.entity.Role;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.dto.CreateSchoolUserRequest;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceSchoolManagementTest {

    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private StudentProfileRepository studentProfileRepository;
    private TeacherProfileRepository teacherProfileRepository;
    private ParentRepository parentRepository;
    private UserService userService;
    private School school;
    private User manager;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        studentProfileRepository = mock(StudentProfileRepository.class);
        teacherProfileRepository = mock(TeacherProfileRepository.class);
        parentRepository = mock(ParentRepository.class);
        userService = new UserService(
                userRepository,
                schoolRepository,
                roleRepository,
                passwordEncoder,
                mock(AuditLogService.class),
                studentProfileRepository,
                teacherProfileRepository,
                parentRepository);

        school = new School();
        school.setId(7L);
        school.setName("Example School");
        school.setActive(true);
        school.setSuspended(false);
        when(schoolRepository.findById(7L)).thenReturn(Optional.of(school));

        Role managerRole = new Role();
        managerRole.setName("MANAGER");
        manager = new User();
        manager.setId(3L);
        manager.setEmail("manager@example.test");
        manager.setSchoolId(7L);
        manager.setActive(true);
        manager.getRoles().add(managerRole);
        when(userRepository.findWithRolesByEmail("manager@example.test")).thenReturn(Optional.of(manager));

        TenantContext.setTenantId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "manager@example.test",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void createsStudentInManagerSchoolAndPersistsRoleAndStudentProfile() {
        Role studentRole = new Role();
        studentRole.setName("STUDENT");
        studentRole.setActive(true);
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.existsByEmailIgnoreCase("student@example.test")).thenReturn(false);
        when(studentProfileRepository.existsBySchoolIdAndAdmissionNumber(7L, "S-102")).thenReturn(false);
        when(passwordEncoder.encode("welcome1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(41L);
            return saved;
        });

        var response = userService.createSchoolUser(studentRequest(), mock(HttpServletRequest.class));

        var savedUser = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals(7L, savedUser.getValue().getSchoolId());
        assertEquals("hashed-password", savedUser.getValue().getPasswordHash());
        assertTrue(savedUser.getValue().getRoles().contains(studentRole));
        assertTrue(school.getUsers().contains(savedUser.getValue()));
        assertEquals(List.of("STUDENT"), response.getRoles());

        var savedProfile = org.mockito.ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(savedProfile.capture());
        assertEquals(41L, savedProfile.getValue().getUserId());
        assertEquals(7L, savedProfile.getValue().getSchoolId());
        assertEquals("S-102", savedProfile.getValue().getAdmissionNumber());
        assertEquals("Avery", savedProfile.getValue().getFirstName());
        assertEquals(LocalDate.now(), savedProfile.getValue().getRegistrationDate());
    }

    @Test
    void rejectsPlatformRoleBeforeSavingAnyUser() {
        CreateSchoolUserRequest request = studentRequest();
        request.setRole("SUPER_ADMIN");

        assertThrows(ForbiddenException.class,
                () -> userService.createSchoolUser(request, mock(HttpServletRequest.class)));

        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName("SUPER_ADMIN");
    }

    @Test
    void rejectsTeacherWithoutRequiredProfileFieldsBeforeSavingAnyUser() {
        CreateSchoolUserRequest request = studentRequest();
        request.setRole("TEACHER");

        assertThrows(BadRequestException.class,
                () -> userService.createSchoolUser(request, mock(HttpServletRequest.class)));

        verify(userRepository, never()).save(any(User.class));
        verify(teacherProfileRepository, never()).save(any());
    }

    @Test
    void createsTeacherWithSchoolRoleAndRequiredTeacherProfile() {
        CreateSchoolUserRequest request = studentRequest();
        request.setRole("TEACHER");
        request.setGender("FEMALE");
        request.setDateOfBirth(LocalDate.of(1990, 4, 12));
        request.setEmploymentStatus("ACTIVE");
        request.setQualification("Bachelor of Education");
        request.setExperienceYears(8);
        request.setAdmissionNumber(null);

        Role teacherRole = new Role();
        teacherRole.setName("TEACHER");
        teacherRole.setActive(true);
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRepository.existsByEmailIgnoreCase("student@example.test")).thenReturn(false);
        when(passwordEncoder.encode("welcome1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        var response = userService.createSchoolUser(request, mock(HttpServletRequest.class));

        var savedUser = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals(7L, savedUser.getValue().getSchoolId());
        assertEquals("hashed-password", savedUser.getValue().getPasswordHash());
        assertTrue(savedUser.getValue().getRoles().contains(teacherRole));
        assertEquals(List.of("TEACHER"), response.getRoles());
        assertFalse(response.toString().contains("welcome1"));
        assertFalse(response.toString().contains("hashed-password"));

        var savedProfile = org.mockito.ArgumentCaptor.forClass(TeacherProfile.class);
        verify(teacherProfileRepository).save(savedProfile.capture());
        assertEquals(42L, savedProfile.getValue().getUserId());
        assertEquals(7L, savedProfile.getValue().getSchoolId());
        assertEquals("FEMALE", savedProfile.getValue().getGender());
        assertEquals(LocalDate.of(1990, 4, 12), savedProfile.getValue().getDateOfBirth());
        assertEquals("ACTIVE", savedProfile.getValue().getEmploymentStatus());
        assertEquals("Bachelor of Education", savedProfile.getValue().getQualification());
        assertEquals(8, savedProfile.getValue().getExperienceYears());
    }

    @Test
    void bindsTeacherCreationJsonToTheExistingRequestDto() throws Exception {
        String payload = """
                {
                  "email": "teacher@example.test",
                  "password": "temporary-test-password",
                  "firstName": "Taylor",
                  "lastName": "Teacher",
                  "phone": "+15550000000",
                  "role": "TEACHER",
                  "gender": "FEMALE",
                  "dateOfBirth": "1990-04-12",
                  "employmentStatus": "ACTIVE",
                  "qualification": "Bachelor of Education",
                  "experienceYears": 8
                }
                """;

        CreateSchoolUserRequest request = new ObjectMapper()
                .findAndRegisterModules()
                .readValue(payload, CreateSchoolUserRequest.class);

        assertEquals("TEACHER", request.getRole());
        assertEquals(LocalDate.of(1990, 4, 12), request.getDateOfBirth());
        assertEquals(8, request.getExperienceYears());
    }

    @Test
    void rejectsDuplicateEmailWithoutCreatingAccount() {
        when(userRepository.existsByEmailIgnoreCase("student@example.test")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> userService.createSchoolUser(studentRequest(), mock(HttpServletRequest.class)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsManagerWhenTenantAssignmentDoesNotMatchPersistedSchool() {
        TenantContext.setTenantId(8L);

        assertThrows(ForbiddenException.class,
                () -> userService.listUsers(org.springframework.data.domain.PageRequest.of(0, 10), null, null, null));

        verify(userRepository, never()).findSchoolUsers(any(), any(), any(), any(), any());
    }

    @Test
    void listsSchoolUsersWithFiltersAndPaginationFromTheManagerTenant() {
        var pageable = PageRequest.of(1, 25);
        User schoolUser = new User();
        schoolUser.setId(41L);
        schoolUser.setEmail("student@example.test");
        schoolUser.setFirstName("Avery");
        schoolUser.setLastName("Student");
        schoolUser.setSchoolId(7L);
        Role studentRole = new Role();
        studentRole.setName("STUDENT");
        schoolUser.getRoles().add(studentRole);
        when(userRepository.findSchoolUsers(7L, "Avery", "STUDENT", false, pageable))
                .thenReturn(new PageImpl<>(List.of(schoolUser), pageable, 26));

        var result = userService.listUsers(pageable, " Avery ", " student ", false);

        assertEquals(1, result.getContent().size());
        assertEquals(41L, result.getContent().get(0).getId());
        assertEquals(7L, result.getContent().get(0).getSchoolId());
        assertEquals("Example School", result.getContent().get(0).getSchoolName());
        assertEquals(List.of("STUDENT"), result.getContent().get(0).getRoles());
        assertEquals(1, result.getPageNumber());
        assertEquals(25, result.getPageSize());
        assertEquals(26, result.getTotalElements());
        verify(userRepository).findSchoolUsers(7L, "Avery", "STUDENT", false, pageable);
    }

    @Test
    void rejectsIdentityWithoutManagerRole() {
        manager.getRoles().clear();
        Role superAdminRole = new Role();
        superAdminRole.setName("SUPER_ADMIN");
        manager.getRoles().add(superAdminRole);

        assertThrows(ForbiddenException.class,
                () -> userService.listUsers(org.springframework.data.domain.PageRequest.of(0, 10), null, null, null));

        verify(userRepository, never()).findSchoolUsers(any(), any(), any(), any(), any());
    }

    @Test
    void cannotFetchUserFromAnotherSchool() {
        when(userRepository.findByIdAndSchoolIdAndDeletedAtIsNull(52L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUser(52L));

        verify(userRepository).findByIdAndSchoolIdAndDeletedAtIsNull(52L, 7L);
    }

    private CreateSchoolUserRequest studentRequest() {
        CreateSchoolUserRequest request = new CreateSchoolUserRequest();
        request.setEmail("student@example.test");
        request.setPassword("welcome1");
        request.setFirstName("Avery");
        request.setLastName("Student");
        request.setRole("STUDENT");
        request.setAdmissionNumber("S-102");
        return request;
    }
}
