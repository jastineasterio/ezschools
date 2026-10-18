package com.edumanage.fees.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.fees.dto.CreatePaymentRequest;
import com.edumanage.fees.dto.PaymentDto;
import com.edumanage.fees.entity.FeeAssignment;
import com.edumanage.fees.entity.Payment;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.fees.repository.PaymentRepository;
import com.edumanage.finance.entity.FinancialCategory;
import com.edumanage.finance.entity.FinancialTransaction;
import com.edumanage.finance.repository.FinancialCategoryRepository;
import com.edumanage.finance.repository.FinancialTransactionRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FinancialCategoryRepository financialCategoryRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final AuditLogService auditLogService;

    public PaymentService(PaymentRepository paymentRepository,
                          FeeAssignmentRepository feeAssignmentRepository,
                          StudentProfileRepository studentProfileRepository,
                          FinancialCategoryRepository financialCategoryRepository,
                          FinancialTransactionRepository financialTransactionRepository,
                          AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.feeAssignmentRepository = feeAssignmentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.financialCategoryRepository = financialCategoryRepository;
        this.financialTransactionRepository = financialTransactionRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<PaymentDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Payment> page;
        if (tenantId != null) {
            page = paymentRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = paymentRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<PaymentDto> listByStudent(Long studentId) {
        Long tenantId = TenantContext.getTenantId();
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Long schoolId = tenantId != null ? tenantId : student.getSchoolId();
        return paymentRepository.findBySchoolIdAndStudentId(schoolId, studentId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public PaymentDto create(CreatePaymentRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        if (request.getReceiptNumber() != null &&
                paymentRepository.existsBySchoolIdAndReceiptNumber(tenantId, request.getReceiptNumber())) {
            throw new com.edumanage.exception.ConflictException("Receipt number already exists");
        }

        FeeAssignment assignment = null;
        if (request.getFeeAssignmentId() != null) {
            assignment = feeAssignmentRepository.findById(request.getFeeAssignmentId())
                    .filter(a -> !a.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Fee assignment not found"));

            if (!tenantId.equals(assignment.getSchoolId())) {
                throw new ForbiddenException("Fee assignment does not belong to this school");
            }

            BigDecimal newBalance = assignment.getBalance().subtract(request.getAmount());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Payment amount exceeds outstanding balance");
            }

            assignment.setAmountPaid(assignment.getAmountPaid().add(request.getAmount()));
            assignment.setBalance(newBalance);
            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                assignment.setStatus("PAID");
            }
            feeAssignmentRepository.save(assignment);
        }

        Payment payment = new Payment();
        payment.setSchoolId(tenantId);
        payment.setStudentId(request.getStudentId());
        payment.setFeeAssignmentId(request.getFeeAssignmentId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentDate(request.getPaymentDate());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setReceiptNumber(request.getReceiptNumber());
        payment.setRemarks(request.getRemarks());
        payment.setStatus("COMPLETED");

        paymentRepository.save(payment);

        FinancialCategory feeCategory = financialCategoryRepository.findBySchoolIdAndCategoryType(tenantId, "INCOME")
                .stream().filter(c -> "FEE".equals(c.getName()) || "FEES".equals(c.getName()))
                .findFirst()
                .orElse(null);

        if (feeCategory != null) {
            FinancialTransaction transaction = new FinancialTransaction();
            transaction.setSchoolId(tenantId);
            transaction.setCategoryId(feeCategory.getId());
            transaction.setAmount(request.getAmount());
            transaction.setTransactionType("INCOME");
            transaction.setTransactionDate(request.getPaymentDate());
            transaction.setReferenceNumber(request.getReceiptNumber());
            transaction.setDescription("Fee payment from student");
            transaction.setPaymentId(payment.getId());
            transaction.setFeeAssignmentId(request.getFeeAssignmentId());
            transaction.setStatus("COMPLETED");
            financialTransactionRepository.save(transaction);
        }

        auditLogService.log("PAYMENT_CREATE", "Payment", payment.getId(),
                "Recorded payment of " + request.getAmount() + " for student", null, null, null);

        return mapToDto(payment);
    }

    private PaymentDto mapToDto(Payment entity) {
        PaymentDto dto = new PaymentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());
        dto.setFeeAssignmentId(entity.getFeeAssignmentId());
        dto.setAmount(entity.getAmount());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setPaymentDate(entity.getPaymentDate());
        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setReceiptNumber(entity.getReceiptNumber());
        dto.setRemarks(entity.getRemarks());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
