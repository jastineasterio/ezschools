package com.edumanage.classes.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.classes.dto.CreateClassRequest;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ClassService classService;

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
    void createUsesAuthenticatedSchoolAndPersistsClass() {
        when(classRepository.existsBySchoolIdAndName(7L, "Grade 1")).thenReturn(false);
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> {
            Class saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });

        var result = classService.create(new CreateClassRequest("Grade 1", "G1", null, "ACTIVE"));

        assertEquals(12L, result.getId());
        assertEquals(7L, result.getSchoolId());
        assertEquals("Grade 1", result.getName());
        verify(classRepository).save(any(Class.class));
    }

    @Test
    void duplicateNameWithinSchoolReturnsConflictWithUsefulMessage() {
        when(classRepository.existsBySchoolIdAndName(7L, "Grade 1")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> classService.create(new CreateClassRequest("Grade 1", "G1", null, "ACTIVE")));

        assertEquals("Class name 'Grade 1' already exists for this school", exception.getMessage());
        verify(classRepository, never()).save(any(Class.class));
    }

    @Test
    void sameClassCodeCanBeUsedByAnotherSchool() {
        when(classRepository.existsBySchoolIdAndName(7L, "Grade 1")).thenReturn(false);
        when(classRepository.existsBySchoolIdAndName(8L, "Grade 1")).thenReturn(false);
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));

        classService.create(new CreateClassRequest("Grade 1", "G1", null, "ACTIVE"));
        TenantContext.setTenantId(8L);
        classService.create(new CreateClassRequest("Grade 1", "G1", null, "ACTIVE"));

        verify(classRepository).existsBySchoolIdAndName(7L, "Grade 1");
        verify(classRepository).existsBySchoolIdAndName(8L, "Grade 1");
        verify(classRepository, org.mockito.Mockito.times(2)).save(any(Class.class));
    }

    @Test
    void managerCannotReadOrUpdateAnotherSchoolsClass() {
        Class otherSchoolClass = new Class();
        otherSchoolClass.setId(14L);
        otherSchoolClass.setSchoolId(8L);
        otherSchoolClass.setName("Grade 2");
        when(classRepository.findById(14L)).thenReturn(Optional.of(otherSchoolClass));

        assertThrows(ForbiddenException.class, () -> classService.getById(14L));
        assertThrows(ForbiddenException.class,
                () -> classService.update(14L, new CreateClassRequest("Grade 2", "G2", null, "ACTIVE")));
        verify(classRepository, never()).save(any(Class.class));
    }

    @Test
    void managerWithoutSchoolAssignmentCannotListAcrossSchools() {
        TenantContext.clear();

        assertThrows(ForbiddenException.class, () -> classService.list(PageRequest.of(0, 10), null));
        verify(classRepository, never()).findAll(any(PageRequest.class));
    }
}
