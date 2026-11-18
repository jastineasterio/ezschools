package com.edumanage.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOverviewDto {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long deletedUsers;
    private long totalSchools;
    private double averageUsersPerSchool;
    private long newUsersPeriod;
    private Double growthPercent;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private List<RoleDistributionDto> roleDistribution;
    private List<ActiveInactiveDto> statusDistribution;
}
