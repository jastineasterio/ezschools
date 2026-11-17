package com.edumanage.examinationtypes.service;

import com.edumanage.examinationtypes.dto.CreateExaminationTypeRequest;
import com.edumanage.examinationtypes.dto.ExaminationTypeDto;
import com.edumanage.examinationtypes.entity.ExaminationType;
import com.edumanage.examinationtypes.repository.ExaminationTypeRepository;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExaminationTypeServiceTest {

    private ExaminationTypeRepository examinationTypeRepository;
    private ExaminationRepository examinationRepository;
    private AuditLogService auditLogService;
    private ExaminationTypeService examinationTypeService;

    @BeforeEach
    void setUp() {
        examinationTypeRepository = mock(ExaminationTypeRepository.class);
        examinationRepository = mock(ExaminationRepository.class);
        auditLogService = mock(AuditLogService.class);
        examinationTypeService = new ExaminationTypeService(
                examinationTypeRepository, examinationRepository, auditLogService);
        TenantContext.setTenantId(7L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    private ExaminationType type(Long id, String name, Boolean active) {
        ExaminationType type = new ExaminationType();
        type.setId(id);
        type.setSchoolId(7L);
        type.setName(name);
        type.setActive(active);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());
        return type;
    }

    @Test
    void createsExaminationTypeForCurrentSchool() {
        CreateExaminationTypeRequest request = new CreateExaminationTypeRequest();
        request.setName("Midterm Examination");
        request.setDescription("Midterm test");
        request.setActive(true);
        when(examinationTypeRepository.existsBySchoolIdAndNameIgnoreCase(7L, "Midterm Examination"))
                .thenReturn(false);
        ExaminationType saved = type(1L, "Midterm Examination", true);
        when(examinationTypeRepository.save(any(ExaminationType.class))).thenReturn(saved);

        ExaminationTypeDto dto = examinationTypeService.create(request);

        assertNotNull(dto);
        assertEquals("Midterm Examination", dto.getName());
        assertEquals(7L, dto.getSchoolId());
        assertTrue(dto.getActive());
        verify(examinationTypeRepository).save(any(ExaminationType.class));
    }

    @Test
    void rejectsDuplicateNameCaseInsensitive() {
        CreateExaminationTypeRequest request = new CreateExaminationTypeRequest();
        request.setName("midterm examination");
        when(examinationTypeRepository.existsBySchoolIdAndNameIgnoreCase(7L, "midterm examination"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> examinationTypeService.create(request));
        verify(examinationTypeRepository, never()).save(any(ExaminationType.class));
    }

    @Test
    void listsOnlyActiveTypesWhenActiveTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        ExaminationType type = type(1L, "Midterm Examination", true);
        when(examinationTypeRepository.findBySchoolIdAndActiveTrueAndDeletedAtIsNull(7L, pageable))
                .thenReturn(new PageImpl<>(List.of(type), pageable, 1));

        PageResponse<ExaminationTypeDto> result = examinationTypeService.list(pageable, null, true);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Midterm Examination", result.getContent().get(0).getName());
    }

    @Test
    void getByIdForbiddenForOtherSchool() {
        ExaminationType type = type(1L, "Midterm Examination", true);
        type.setSchoolId(99L);
        when(examinationTypeRepository.findById(1L)).thenReturn(Optional.of(type));

        assertThrows(ForbiddenException.class, () -> examinationTypeService.getById(1L));
    }

    @Test
    void updateRejectsDuplicateNameExcludingItself() {
        ExaminationType existing = type(1L, "Midterm Examination", true);
        when(examinationTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        CreateExaminationTypeRequest request = new CreateExaminationTypeRequest();
        request.setName("Final Examination");
        request.setActive(true);
        when(examinationTypeRepository.existsBySchoolIdAndNameIgnoreCaseAndIdNot(7L, "Final Examination", 1L))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> examinationTypeService.update(1L, request));
        verify(examinationTypeRepository, never()).save(any(ExaminationType.class));
    }

    @Test
    void setActiveTogglesStatus() {
        ExaminationType existing = type(1L, "Midterm Examination", true);
        when(examinationTypeRepository.findById(1L)).thenReturn(Optional.of(existing));

        ExaminationTypeDto dto = examinationTypeService.setActive(1L, false);

        assertEquals(false, dto.getActive());
        verify(examinationTypeRepository).save(existing);
    }

    @Test
    void deletePreventedWhenReferencedByExamination() {
        when(examinationTypeRepository.findById(1L)).thenReturn(Optional.of(type(1L, "Midterm Examination", true)));
        when(examinationRepository.existsBySchoolIdAndTypeId(7L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> examinationTypeService.delete(1L));
        verify(examinationTypeRepository, never()).save(any(ExaminationType.class));
    }

    @Test
    void deleteRemovesTypeWhenNotReferenced() {
        ExaminationType existing = type(1L, "Midterm Examination", true);
        when(examinationTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(examinationRepository.existsBySchoolIdAndTypeId(7L, 1L)).thenReturn(false);

        examinationTypeService.delete(1L);

        assertNotNull(existing.getDeletedAt());
        verify(examinationTypeRepository).save(existing);
    }

    @Test
    void searchFiltersByName() {
        Pageable pageable = PageRequest.of(0, 10);
        ExaminationType type = type(1L, "Midterm Examination", true);
        when(examinationTypeRepository.findBySchoolIdAndDeletedAtIsNullAndNameContainingIgnoreCase(
                7L, "midterm", pageable)).thenReturn(new PageImpl<>(List.of(type), pageable, 1));

        PageResponse<ExaminationTypeDto> result = examinationTypeService.list(pageable, "midterm", null);

        assertEquals(1, result.getContent().size());
    }
}
