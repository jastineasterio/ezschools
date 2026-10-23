package com.edumanage.academicyears.service;

import com.edumanage.academicyears.dto.CreateAcademicYearRequest;
import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademicYearServiceTest {

    private AcademicYearRepository academicYearRepository;
    private AcademicYearService academicYearService;

    @BeforeEach
    void setUp() {
        academicYearRepository = mock(AcademicYearRepository.class);
        academicYearService = new AcademicYearService(
                academicYearRepository,
                mock(AuditLogService.class));
        TenantContext.setTenantId(7L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void rejectsStartDateAfterEndDate() {
        CreateAcademicYearRequest request = request(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 8, 31));

        assertThrows(BadRequestException.class, () -> academicYearService.create(request));
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void rejectsOverlappingSchoolAcademicYearDates() {
        CreateAcademicYearRequest request = request(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2027, 7, 31));
        when(academicYearRepository.existsBySchoolIdAndName(7L, request.getName())).thenReturn(false);
        when(academicYearRepository.existsOverlapping(
                7L, request.getStartDate(), request.getEndDate(), null)).thenReturn(true);

        assertThrows(ConflictException.class, () -> academicYearService.create(request));
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void doesNotSilentlyReplaceAnExistingCurrentAcademicYear() {
        CreateAcademicYearRequest request = request(
                LocalDate.of(2027, 8, 1),
                LocalDate.of(2028, 7, 31));
        request.setName("2027-2028");
        request.setIsCurrent(true);
        AcademicYear currentYear = new AcademicYear();
        currentYear.setId(10L);
        currentYear.setSchoolId(7L);
        currentYear.setCurrent(true);
        when(academicYearRepository.existsBySchoolIdAndName(7L, request.getName())).thenReturn(false);
        when(academicYearRepository.existsOverlapping(
                7L, request.getStartDate(), request.getEndDate(), null)).thenReturn(false);
        when(academicYearRepository.findBySchoolIdAndIsCurrentTrueAndDeletedAtIsNull(7L))
                .thenReturn(Optional.of(currentYear));

        assertThrows(ConflictException.class, () -> academicYearService.create(request));
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
        verify(academicYearRepository, never()).save(currentYear);
    }

    @Test
    void listsAcademicYearsForTheCurrentTenantWithSearchAndPagination() {
        var pageable = PageRequest.of(1, 5);
        AcademicYear academicYear = new AcademicYear();
        academicYear.setId(12L);
        academicYear.setSchoolId(7L);
        academicYear.setName("2026-2027");
        academicYear.setStartDate(LocalDate.of(2026, 8, 1));
        academicYear.setEndDate(LocalDate.of(2027, 7, 31));
        when(academicYearRepository.search(7L, "2026", pageable))
                .thenReturn(new PageImpl<>(List.of(academicYear), pageable, 6));

        var result = academicYearService.listBySchool(pageable, " 2026 ");

        assertNotNull(result);
        assertEquals(12L, result.getContent().get(0).getId());
        assertEquals(1, result.getPageNumber());
        assertEquals(5, result.getPageSize());
        assertEquals(6, result.getTotalElements());
        verify(academicYearRepository).search(7L, "2026", pageable);
    }

    private CreateAcademicYearRequest request(LocalDate startDate, LocalDate endDate) {
        return new CreateAcademicYearRequest("2026-2027", startDate, endDate, "UPCOMING", false);
    }
}
