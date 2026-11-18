package com.edumanage.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageUserDistributionDto {
    private Long packageId;
    private String packageName;
    private String displayName;
    private boolean unassigned;
    private long schoolCount;
    private long totalUsers;
    private double averageUsersPerSchool;
    private long activeUsers;
    private long inactiveUsers;
    private Map<String, Long> roleDistribution;
}
