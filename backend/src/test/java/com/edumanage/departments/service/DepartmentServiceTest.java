package com.edumanage.departments.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.departments.dto.CreateDepartmentRequest;
import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.roles.entity.Role;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContext.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRejectsHeadFromAnotherSchool() {
        when(userRepository.findByIdAndSchoolIdAndDeletedAtIsNull(12L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                departmentService.create(new CreateDepartmentRequest("Languages", "LANG", null, 12L, "ACTIVE")));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createRejectsSameSchoolUserWithoutActiveHodRole() {
        User user = new User();
        user.setSchoolId(7L);
        user.setActive(true);
        when(userRepository.findByIdAndSchoolIdAndDeletedAtIsNull(12L, 7L))
                .thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () ->
                departmentService.create(new CreateDepartmentRequest("Languages", "LANG", null, 12L, "ACTIVE")));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveHod() {
        User user = new User();
        user.setSchoolId(7L);
        user.setActive(false);
        Role hodRole = new Role();
        hodRole.setName("HOD");
        user.getRoles().add(hodRole);
        when(userRepository.findByIdAndSchoolIdAndDeletedAtIsNull(12L, 7L))
                .thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () ->
                departmentService.create(new CreateDepartmentRequest("Languages", "LANG", null, 12L, "ACTIVE")));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void listEligibleHeadsUsesAuthenticatedSchoolAndReturnsNames() {
        User user = new User();
        user.setId(12L);
        user.setSchoolId(7L);
        user.setFirstName("Amina");
        user.setLastName("Otieno");
        user.setEmail("amina@example.test");
        when(userRepository.findActiveBySchoolAndRoleNames(7L, List.of("HOD")))
                .thenReturn(List.of(user));

        var result = departmentService.listEligibleHeads();

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).id());
        assertEquals("Amina Otieno", result.get(0).name());
        assertEquals("amina@example.test", result.get(0).email());
        verify(userRepository).findActiveBySchoolAndRoleNames(7L, List.of("HOD"));
    }
}
