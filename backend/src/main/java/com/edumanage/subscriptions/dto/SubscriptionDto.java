package com.edumanage.subscriptions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String schoolName;
    private Long packageId;
    private String packageName;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String billingPeriod;
    private BigDecimal price;
    private String paymentStatus;
    private LocalDateTime trialEndsAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
}
