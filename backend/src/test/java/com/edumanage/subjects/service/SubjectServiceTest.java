package com.edumanage.subjects.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.departments.entity.Department;
import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.subjects.dto.CreateSubjectRequest;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SubjectService subjectService;

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
    void createRejectsDepartmentOutsideAuthenticatedSchool() {
        when(departmentRepository.findBySchoolIdAndId(7L, 9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                subjectService.create(new CreateSubjectRequest("Kiswahili", "KISW", null, 9L, "ACTIVE")));

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createPersistsSubjectWithValidatedDepartment() {
        Department department = new Department();
        department.setId(9L);
        department.setSchoolId(7L);
        department.setName("Languages");
        when(departmentRepository.findBySchoolIdAndId(7L, 9L)).thenReturn(Optional.of(department));
        when(subjectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = subjectService.create(
                new CreateSubjectRequest("Kiswahili", "KISW", null, 9L, "ACTIVE"));

        assertEquals(7L, result.getSchoolId());
        assertEquals(9L, result.getDepartmentId());
        assertEquals("Languages", result.getDepartmentName());
        verify(subjectRepository).save(any());
    }
}
