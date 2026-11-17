package com.edumanage.gradingschemes.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.gradingschemes.dto.CreateGradingSchemeRequest;
import com.edumanage.gradingschemes.dto.CreateGradingSchemeRangeRequest;
import com.edumanage.gradingschemes.dto.GradingSchemeDto;
import com.edumanage.gradingschemes.entity.GradingScheme;
import com.edumanage.gradingschemes.repository.GradingSchemeRepository;
import com.edumanage.gradingschemeranges.entity.GradingSchemeRange;
import com.edumanage.gradingschemeranges.repository.GradingSchemeRangeRepository;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GradingSchemeServiceTest {

    private GradingSchemeRepository gradingSchemeRepository;
    private GradingSchemeRangeRepository gradingSchemeRangeRepository;
    private ExaminationSubjectRepository examinationSubjectRepository;
    private AuditLogService auditLogService;
    private GradingSchemeService service;

    private final Long schoolId = 7L;
    private final Long schemeId = 60L;

    @BeforeEach
    void setUp() {
        gradingSchemeRepository = mock(GradingSchemeRepository.class);
        gradingSchemeRangeRepository = mock(GradingSchemeRangeRepository.class);
        examinationSubjectRepository = mock(ExaminationSubjectRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new GradingSchemeService(
                gradingSchemeRepository, gradingSchemeRangeRepository,
                examinationSubjectRepository, auditLogService);
        TenantContext.setTenantId(schoolId);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private GradingScheme scheme(Long id, Long schoolId, String name) {
        GradingScheme s = new GradingScheme();
        s.setId(id);
        s.setSchoolId(schoolId);
        s.setName(name);
        s.setStatus("ACTIVE");
        s.setDeletedAt(null);
        return s;
    }

    private GradingSchemeRange range(Long id, String grade, String min, String max) {
        GradingSchemeRange r = new GradingSchemeRange();
        r.setId(id);
        r.setSchemeId(schemeId);
        r.setGrade(grade);
        r.setMinMark(new BigDecimal(min));
        r.setMaxMark(new BigDecimal(max));
        r.setDeletedAt(null);
        return r;
    }

    @Test
    void createSetsSchoolIdFromTenantContext() {
        CreateGradingSchemeRequest request = new CreateGradingSchemeRequest(
                "MJ Secondary Grading", "Secondary school grading", false, "ACTIVE");

        when(gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(schoolId))
                .thenReturn(Optional.empty());
        GradingScheme saved = scheme(schemeId, schoolId, "MJ Secondary Grading");
        when(gradingSchemeRepository.save(any(GradingScheme.class))).thenReturn(saved);

        GradingSchemeDto dto = service.create(request);

        assertNotNull(dto);
        assertEquals("MJ Secondary Grading", dto.getName());
        assertEquals(schoolId, dto.getSchoolId());
        assertEquals("ACTIVE", dto.getStatus());
        verify(gradingSchemeRepository).save(any(GradingScheme.class));
    }

    @Test
    void createThenAddRangesSavesSchemeAndRangesConsistently() {
        CreateGradingSchemeRequest schemeRequest = new CreateGradingSchemeRequest(
                "O Level Grading", "Secondary grading", false, "ACTIVE");
        when(gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(schoolId))
                .thenReturn(Optional.empty());
        GradingScheme saved = scheme(schemeId, schoolId, "O Level Grading");
        when(gradingSchemeRepository.save(any(GradingScheme.class))).thenReturn(saved);

        GradingSchemeDto created = service.create(schemeRequest);

        assertNotNull(created);
        assertEquals(schoolId, created.getSchoolId());

        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(saved));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of());

        CreateGradingSchemeRangeRequest range1 = new CreateGradingSchemeRangeRequest();
        range1.setSchemeId(schemeId);
        range1.setGrade("F");
        range1.setMinMark(0);
        range1.setMaxMark(10);

        CreateGradingSchemeRangeRequest range2 = new CreateGradingSchemeRangeRequest();
        range2.setSchemeId(schemeId);
        range2.setGrade("D");
        range2.setMinMark(11);
        range2.setMaxMark(20);

        service.addRange(schemeId, range1);
        service.addRange(schemeId, range2);

        verify(gradingSchemeRepository, times(1)).save(any(GradingScheme.class));
        verify(gradingSchemeRangeRepository, times(2)).save(any(GradingSchemeRange.class));

        ArgumentCaptor<GradingSchemeRange> rangeCaptor = ArgumentCaptor.forClass(GradingSchemeRange.class);
        verify(gradingSchemeRangeRepository, times(2)).save(rangeCaptor.capture());
        List<GradingSchemeRange> savedRanges = rangeCaptor.getAllValues();
        assertEquals(2, savedRanges.size());
        assertTrue(savedRanges.stream().allMatch(range -> schemeId.equals(range.getSchemeId())));
        assertEquals(List.of("F", "D"), savedRanges.stream().map(GradingSchemeRange::getGrade).toList());
    }

    @Test
    void addRangeRejectsNullSchemeIdWithoutTouchingPersistence() {
        CreateGradingSchemeRangeRequest request = new CreateGradingSchemeRangeRequest();
        request.setSchemeId(schemeId);
        request.setGrade("A");
        request.setMinMark(0);
        request.setMaxMark(10);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.addRange(null, request));
        assertTrue(ex.getMessage().contains("Scheme ID"));

        verify(gradingSchemeRepository, never()).findById(any());
        verify(gradingSchemeRangeRepository, never()).save(any(GradingSchemeRange.class));
        verify(gradingSchemeRangeRepository, never()).findBySchemeIdOrderByMinMarkDesc(any());
    }

    @Test
    void multipleRangesAddedToSameSchemeWithoutCreatingAnotherScheme() {
        GradingScheme saved = scheme(schemeId, schoolId, "O Level Grading");
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(saved));

        CreateGradingSchemeRangeRequest range1 = new CreateGradingSchemeRangeRequest();
        range1.setSchemeId(schemeId);
        range1.setGrade("B");
        range1.setMinMark(0);
        range1.setMaxMark(74);

        GradingSchemeRange persistedB = range(1L, "B", "0", "74");
        when(gradingSchemeRangeRepository.save(any(GradingSchemeRange.class))).thenReturn(persistedB);
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of());

        service.addRange(schemeId, range1);

        CreateGradingSchemeRangeRequest range2 = new CreateGradingSchemeRangeRequest();
        range2.setSchemeId(schemeId);
        range2.setGrade("A");
        range2.setMinMark(75);
        range2.setMaxMark(100);

        when(gradingSchemeRangeRepository.save(any(GradingSchemeRange.class)))
                .thenReturn(range(2L, "A", "75", "100"));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(persistedB));

        service.addRange(schemeId, range2);

        verify(gradingSchemeRepository, never()).save(any(GradingScheme.class));
        verify(gradingSchemeRangeRepository, times(2)).save(any(GradingSchemeRange.class));

        ArgumentCaptor<GradingSchemeRange> rangeCaptor = ArgumentCaptor.forClass(GradingSchemeRange.class);
        verify(gradingSchemeRangeRepository, times(2)).save(rangeCaptor.capture());
        assertTrue(rangeCaptor.getAllValues().stream().allMatch(range -> schemeId.equals(range.getSchemeId())));
    }

    @Test
    void addRangeValidationFailureStopsChainWithoutDuplicatePersistence() {
        GradingScheme saved = scheme(schemeId, schoolId, "O Level Grading");
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(saved));

        GradingSchemeRange persistedB = range(1L, "B", "0", "74");
        GradingSchemeRange persistedInvalid = range(2L, "A", "80", "100");
        when(gradingSchemeRangeRepository.save(any(GradingSchemeRange.class))).thenReturn(persistedB);
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(), List.of(persistedB, persistedInvalid));

        CreateGradingSchemeRangeRequest valid = new CreateGradingSchemeRangeRequest();
        valid.setSchemeId(schemeId);
        valid.setGrade("B");
        valid.setMinMark(0);
        valid.setMaxMark(74);

        CreateGradingSchemeRangeRequest invalid = new CreateGradingSchemeRangeRequest();
        invalid.setSchemeId(schemeId);
        invalid.setGrade("A");
        invalid.setMinMark(80);
        invalid.setMaxMark(100);

        service.addRange(schemeId, valid);

        assertThrows(BadRequestException.class, () -> service.addRange(schemeId, invalid));

        verify(gradingSchemeRangeRepository, times(2)).save(any(GradingSchemeRange.class));
    }

    @Test
    void addRangeSavesMultipleCustomGradeRanges() {
        GradingScheme scheme = scheme(schemeId, schoolId, "O Level Grading");
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));

        CreateGradingSchemeRangeRequest request = new CreateGradingSchemeRangeRequest();
        request.setSchemeId(schemeId);
        request.setGrade("A");
        request.setMinMark(40);
        request.setMaxMark(50);
        request.setRemark("Excellent");

        GradingSchemeRange saved = range(1L, "A", "40", "50");
        when(gradingSchemeRangeRepository.save(any(GradingSchemeRange.class))).thenReturn(saved);
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of());

        service.addRange(schemeId, request);

        verify(gradingSchemeRangeRepository).save(any(GradingSchemeRange.class));
    }

    @Test
    void addRangeRejectsNegativeMinimumMark() {
        GradingScheme scheme = scheme(schemeId, schoolId, "Grading");
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));

        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(range(1L, "A", "-5", "50")));

        CreateGradingSchemeRangeRequest request = new CreateGradingSchemeRangeRequest();
        request.setSchemeId(schemeId);
        request.setGrade("A");
        request.setMinMark(-5);
        request.setMaxMark(50);

        when(gradingSchemeRangeRepository.save(any(GradingSchemeRange.class)))
                .thenReturn(range(1L, "A", "-5", "50"));

        assertThrows(BadRequestException.class, () -> service.addRange(schemeId, request));
    }

    @Test
    void addRangeRejectsMinimumGreaterThanMaximum() {
        GradingScheme scheme = scheme(schemeId, schoolId, "Grading");
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));

        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(range(1L, "A", "40", "30")));

        CreateGradingSchemeRangeRequest request = new CreateGradingSchemeRangeRequest();
        request.setSchemeId(schemeId);
        request.setGrade("A");
        request.setMinMark(40);
        request.setMaxMark(30);

        when(gradingSchemeRangeRepository.save(any(GradingSchemeRange.class)))
                .thenReturn(range(1L, "A", "40", "30"));

        assertThrows(BadRequestException.class, () -> service.addRange(schemeId, request));
    }

    @Test
    void addRangeRejectsOverlappingRanges() {
        GradingScheme scheme = scheme(schemeId, schoolId, "Grading");
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));

        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(
                        range(1L, "A", "40", "50"),
                        range(2L, "B", "45", "49")));

        CreateGradingSchemeRangeRequest request = new CreateGradingSchemeRangeRequest();
        request.setSchemeId(schemeId);
        request.setGrade("B");
        request.setMinMark(45);
        request.setMaxMark(49);

        when(gradingSchemeRangeRepository.save(any(GradingSchemeRange.class)))
                .thenReturn(range(2L, "B", "45", "49"));

        assertThrows(BadRequestException.class, () -> service.addRange(schemeId, request));
    }

    @Test
    void addRangeRejectsGapInRanges() {
        GradingScheme scheme = scheme(schemeId, schoolId, "Grading");
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));

        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(
                        range(1L, "B", "0", "20"),
                        range(2L, "A", "30", "50")));

        CreateGradingSchemeRangeRequest request = new CreateGradingSchemeRangeRequest();
        request.setSchemeId(schemeId);
        request.setGrade("A");
        request.setMinMark(30);
        request.setMaxMark(50);

        when(gradingSchemeRangeRepository.save(any(GradingSchemeRange.class)))
                .thenReturn(range(2L, "A", "30", "50"));

        assertThrows(BadRequestException.class, () -> service.addRange(schemeId, request));
    }

    @Test
    void getByIdForbiddenForOtherSchool() {
        GradingScheme other = scheme(99L, 99L, "Other School Scheme");
        when(gradingSchemeRepository.findById(99L)).thenReturn(Optional.of(other));

        assertThrows(ForbiddenException.class, () -> service.getById(99L));
    }

    @Test
    void validateRangesAgainstMaximumMarksRejectsRangeExceedingMaximum() {
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme(schemeId, schoolId, "Scheme")));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(range(1L, "A", "0", "100")));

        assertThrows(BadRequestException.class,
                () -> service.validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50")));
    }

    @Test
    void validateRangesAgainstMaximumMarksRejectsCoverageBelowMaximum() {
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme(schemeId, schoolId, "Scheme")));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(range(1L, "A", "0", "40")));

        assertThrows(BadRequestException.class,
                () -> service.validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50")));
    }

    @Test
    void validateRangesAgainstMaximumMarksAcceptsExactCoverage() {
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme(schemeId, schoolId, "Scheme")));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(
                        range(1L, "F", "0", "17"),
                        range(2L, "D", "18", "24"),
                        range(3L, "C", "25", "31"),
                        range(4L, "B", "32", "39"),
                        range(5L, "A", "40", "50")));

        service.validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50"));
    }

    @Test
    void validateRangesAgainstMaximumMarksForbiddenForOtherSchool() {
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme(schemeId, 99L, "Other")));

        assertThrows(ForbiddenException.class,
                () -> service.validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50")));
    }

    @Test
    void validateRangesAgainstMaximumMarksRejectsNonZeroStart() {
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme(schemeId, schoolId, "Scheme")));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(range(1L, "A", "5", "50")));

        assertThrows(BadRequestException.class,
                () -> service.validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50")));
    }

    @Test
    void schemeNotFoundThrowsResourceNotFound() {
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50")));
    }

    @Test
    void createWithDefaultTrueSetsSchemeDefault() {
        CreateGradingSchemeRequest request = new CreateGradingSchemeRequest(
                "Default Scheme", "The default", true, "ACTIVE");

        when(gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(schoolId))
                .thenReturn(Optional.empty());
        GradingScheme saved = scheme(schemeId, schoolId, "Default Scheme");
        saved.setDefault(true);
        when(gradingSchemeRepository.save(any(GradingScheme.class))).thenReturn(saved);

        GradingSchemeDto dto = service.create(request);

        assertNotNull(dto);
        assertTrue(dto.isDefault(), "Created scheme marked as default should expose isDefault=true");
        assertEquals("Default Scheme", dto.getName());
        assertEquals(schoolId, dto.getSchoolId());
        verify(gradingSchemeRepository).save(any(GradingScheme.class));
    }

    @Test
    void createWithDefaultTrueUnsetsPreviousDefault() {
        CreateGradingSchemeRequest request = new CreateGradingSchemeRequest(
                "New Default", "New default", true, "ACTIVE");

        GradingScheme existingDefault = scheme(10L, schoolId, "Old Default");
        existingDefault.setDefault(true);
        when(gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(schoolId))
                .thenReturn(Optional.of(existingDefault));
        GradingScheme saved = scheme(schemeId, schoolId, "New Default");
        when(gradingSchemeRepository.save(any(GradingScheme.class))).thenReturn(saved);

        GradingSchemeDto dto = service.create(request);

        assertNotNull(dto);
        assertTrue(dto.isDefault(), "New default scheme should expose isDefault=true");
        assertFalse(existingDefault.isDefault(), "Previous default should be unset for the school");
        assertEquals("New Default", dto.getName());
        assertEquals(schoolId, dto.getSchoolId());
        verify(gradingSchemeRepository, times(2)).save(any(GradingScheme.class));
    }
}
