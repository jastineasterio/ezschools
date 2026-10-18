package com.edumanage.subscriptions.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription extends UuidBaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "TRIAL";

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "billing_period", nullable = false, length = 20)
    private String billingPeriod = "MONTHLY";

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "PENDING";

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;
}
