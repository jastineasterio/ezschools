package com.edumanage.fees.service;

import com.edumanage.fees.entity.FeeStructure;
import com.edumanage.fees.repository.FeeStructureRepository;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeeStructureServiceTest {

    private FeeStructureRepository feeStructureRepository;
    private FeeStructureService feeStructureService;

    @BeforeEach
    void setUp() {
        feeStructureRepository = mock(FeeStructureRepository.class);
        feeStructureService = new FeeStructureService(feeStructureRepository, mock(com.edumanage.audit.service.AuditLogService.class),
                mock(com.edumanage.security.AccessGuard.class));
    }

    @Test
    void testListTenantScoped() {
        TenantContext.setTenantId(1L);
        Page<FeeStructure> page = org.springframework.data.domain.Page.empty();
        when(feeStructureRepository.findBySchoolId(1L, org.springframework.data.domain.Pageable.unpaged())).thenReturn(page);

        var result = feeStructureService.list(org.springframework.data.domain.Pageable.unpaged());
        assertNotNull(result);
        verify(feeStructureRepository).findBySchoolId(1L, org.springframework.data.domain.Pageable.unpaged());
        TenantContext.clear();
    }

    @Test
    void testGetByIdTenantMismatchThrowsForbidden() {
        TenantContext.setTenantId(1L);
        FeeStructure fee = new FeeStructure();
        fee.setId(1L);
        fee.setSchoolId(2L);
        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(fee));

        assertThrows(com.edumanage.exception.ForbiddenException.class, () -> feeStructureService.getById(1L));
        TenantContext.clear();
    }
}
