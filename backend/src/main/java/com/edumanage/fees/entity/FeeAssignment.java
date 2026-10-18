package com.edumanage.fees.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fee_assignments")
@Getter
@Setter
public class FeeAssignment extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "fee_structure_id", nullable = false)
    private Long feeStructureId;

    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "assigned_at", nullable = false)
    private LocalDate assignedAt;
}
