package com.edumanage.fees.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "fee_assignment_id")
    private Long feeAssignmentId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod = "CASH";

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "reference_number", length = 255)
    private String referenceNumber;

    @Column(name = "receipt_number", length = 255)
    private String receiptNumber;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "COMPLETED";
}
