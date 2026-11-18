package com.edumanage.platform.dto;

import com.edumanage.audit.dto.RecentAuditLogDto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private List<RecentSchoolDto> recentSchools;
    private List<RecentAuditLogDto> recentAuditLogs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageDistribution {
        private String name;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSchoolDto {
        private Long id;
        private String uuid;
        private String name;
        private String registrationNumber;
        private boolean isActive;
        private boolean isSuspended;
        private LocalDateTime createdAt;
        private long userCount;
        private String packageName;
    }
}
