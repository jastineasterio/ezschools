package com.edumanage.fees.service;

import com.edumanage.fees.entity.Payment;
import com.edumanage.fees.repository.PaymentRepository;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.finance.repository.FinancialCategoryRepository;
import com.edumanage.finance.repository.FinancialTransactionRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
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

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private FeeAssignmentRepository feeAssignmentRepository;
    private StudentProfileRepository studentProfileRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        feeAssignmentRepository = mock(FeeAssignmentRepository.class);
        studentProfileRepository = mock(StudentProfileRepository.class);
        FinancialCategoryRepository financialCategoryRepository = mock(FinancialCategoryRepository.class);
        FinancialTransactionRepository financialTransactionRepository = mock(FinancialTransactionRepository.class);
        paymentService = new PaymentService(
                paymentRepository,
                feeAssignmentRepository,
                studentProfileRepository,
                financialCategoryRepository,
                financialTransactionRepository,
                mock(com.edumanage.audit.service.AuditLogService.class)
        );
    }

    @Test
    void testListTenantScoped() {
        TenantContext.setTenantId(1L);
        Page<Payment> page = Page.empty();
        when(paymentRepository.findBySchoolId(1L, org.springframework.data.domain.Pageable.unpaged())).thenReturn(page);

        var result = paymentService.list(org.springframework.data.domain.Pageable.unpaged());
        assertNotNull(result);
        verify(paymentRepository).findBySchoolId(1L, org.springframework.data.domain.Pageable.unpaged());
        TenantContext.clear();
    }
}
