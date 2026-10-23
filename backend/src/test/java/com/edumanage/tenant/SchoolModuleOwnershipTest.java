package com.edumanage.tenant;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.boarding.repository.DormitoryRepository;
import com.edumanage.boarding.repository.RoomRepository;
import com.edumanage.boarding.service.DormitoryService;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.departments.dto.CreateDepartmentRequest;
import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.departments.service.DepartmentService;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.streams.dto.CreateStreamRequest;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.streams.service.StreamService;
import com.edumanage.subjects.dto.CreateSubjectRequest;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.subjects.service.SubjectService;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolModuleOwnershipTest {

    @BeforeEach
    void setTenant() {
        TenantContext.clear();
        TenantContext.setTenantId(7L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void streamCannotBeCreatedForAnotherSchoolsClass() {
        StreamRepository streams = mock(StreamRepository.class);
        ClassRepository classes = mock(ClassRepository.class);
        var service = new StreamService(streams, classes, mock(AuditLogService.class));
        Class otherSchoolClass = new Class();
        otherSchoolClass.setId(44L);
        otherSchoolClass.setSchoolId(8L);
        when(classes.findById(44L)).thenReturn(Optional.of(otherSchoolClass));

        assertThrows(ForbiddenException.class,
                () -> service.create(new CreateStreamRequest(44L, "Blue", "BL", 30, "ACTIVE")));
        verify(streams, never()).save(any());
    }

    @Test
    void subjectCannotReferenceAnotherSchoolsDepartment() {
        SubjectRepository subjects = mock(SubjectRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        var service = new SubjectService(subjects, departments, mock(AuditLogService.class));
        when(departments.findBySchoolIdAndId(7L, 44L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(new CreateSubjectRequest("Physics", "PHY", null, 44L, "ACTIVE")));
        verify(subjects, never()).save(any());
    }

    @Test
    void departmentCannotReferenceAnotherSchoolsHead() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        UserRepository users = mock(UserRepository.class);
        var service = new DepartmentService(departments, users, mock(AuditLogService.class));
        User otherSchoolHead = new User();
        otherSchoolHead.setSchoolId(8L);
        when(users.findById(44L)).thenReturn(Optional.of(otherSchoolHead));

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(new CreateDepartmentRequest("Science", "SCI", null, 44L, "ACTIVE")));
        verify(departments, never()).save(any());
    }

    @Test
    void managerCannotDeleteAnotherSchoolsDormitory() {
        DormitoryRepository dormitories = mock(DormitoryRepository.class);
        when(dormitories.findBySchoolIdAndId(7L, 44L)).thenReturn(Optional.empty());
        var service = new DormitoryService(
                dormitories, mock(RoomRepository.class), mock(UserRepository.class), mock(AuditLogService.class));

        assertThrows(ResourceNotFoundException.class, () -> service.deleteDormitory(44L));
        verify(dormitories, never()).save(any());
    }
}
