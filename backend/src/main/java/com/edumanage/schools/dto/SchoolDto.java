package com.edumanage.schools.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolDto {
    private Long id;
    private String uuid;
    private String name;
    private String registrationNumber;
    private String email;
    private String phone;
    private String address;
    private String region;
    private String district;
    private String timeZone;
    private boolean isActive;
    private boolean isSuspended;
    private LocalDateTime suspendedAt;
    private String suspendedReason;
    private LocalDateTime createdAt;
    private long userCount;
    private String packageName;
    private String subscriptionStatus;
    private LocalDateTime subscriptionEndDate;
}
