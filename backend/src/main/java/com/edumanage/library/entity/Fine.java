package com.edumanage.library.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "fines")
public class Fine extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "borrowing_id", nullable = false)
    private Long borrowingId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "fine_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "fine_type", nullable = false, length = 20)
    private String fineType = "OVERDUE";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "paid_date")
    private java.time.LocalDate paidDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
