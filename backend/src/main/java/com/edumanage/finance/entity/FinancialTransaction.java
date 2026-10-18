package com.edumanage.finance.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_transactions")
@Getter
@Setter
public class FinancialTransaction extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "reference_number", length = 255)
    private String referenceNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "fee_assignment_id")
    private Long feeAssignmentId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "COMPLETED";
}
