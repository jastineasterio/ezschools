package com.edumanage.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformDashboardDto {
    private long totalSchools;
    private long activeSchools;
    private long suspendedSchools;
    private long totalUsers;
    private long activeSubscriptions;
    private List<PackageDistribution> packageDistribution;
    private List<Object> recentSchools;
    private List<Object> recentAuditLogs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageDistribution {
        private String name;
        private long count;
    }
}
