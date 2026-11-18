package com.edumanage.platform.service;

import com.edumanage.audit.dto.RecentAuditLogDto;
import com.edumanage.audit.repository.AuditLogRepository;
import com.edumanage.platform.dto.PlatformDashboardDto;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.subscriptions.entity.Subscription;
import com.edumanage.subscriptions.repository.SubscriptionRepository;
import com.edumanage.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformDashboardService {

    private static final int RECENT_SCHOOLS_LIMIT = 5;
    private static final int RECENT_AUDIT_LOGS_LIMIT = 10;

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PackageRepository packageRepository;
    private final AuditLogRepository auditLogRepository;

    public PlatformDashboardDto getDashboard() {
        long totalSchools = schoolRepository.count();
        long activeSchools = schoolRepository.countActiveNonSuspended();
        long suspendedSchools = schoolRepository.countByIsSuspended(true);

        long totalUsers = countSchoolAssociatedUsers();
        long activeSubscriptions = subscriptionRepository.countByStatus("ACTIVE");

        List<PlatformDashboardDto.PackageDistribution> packageDistribution = activePackageDistribution();

        List<PlatformDashboardDto.RecentSchoolDto> recentSchools = recentSchools();
        List<RecentAuditLogDto> recentAuditLogList = recentAuditLogs();

        return new PlatformDashboardDto(
                totalSchools, activeSchools, suspendedSchools, totalUsers, activeSubscriptions,
                packageDistribution, recentSchools, recentAuditLogList);
    }

    private long countSchoolAssociatedUsers() {
        List<Long> schoolIds = schoolRepository.findAll().stream()
                .map(School::getId)
                .collect(Collectors.toList());
        if (schoolIds.isEmpty()) {
            return 0L;
        }
        return userRepository.countSchoolUsersBySchoolIds(schoolIds).stream()
                .mapToLong(UserRepository.SchoolUserCount::getUserCount)
                .sum();
    }

    private List<PlatformDashboardDto.PackageDistribution> activePackageDistribution() {
        List<Object[]> rows = subscriptionRepository.activeSubscriptionDistributionByPackage();
        return rows.stream()
                .map(row -> new PlatformDashboardDto.PackageDistribution(
                        String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<PlatformDashboardDto.RecentSchoolDto> recentSchools() {
        List<School> newest = schoolRepository.findNewest(PageRequest.of(0, RECENT_SCHOOLS_LIMIT));
        List<Long> schoolIds = newest.stream().map(School::getId).toList();
        Map<Long, Long> userCounts = schoolIds.isEmpty()
                ? Map.of()
                : userRepository.countSchoolUsersBySchoolIds(schoolIds).stream()
                        .collect(Collectors.toMap(
                                UserRepository.SchoolUserCount::getSchoolId,
                                UserRepository.SchoolUserCount::getUserCount));

        return newest.stream()
                .map(school -> new PlatformDashboardDto.RecentSchoolDto(
                        school.getId(),
                        school.getUuid() != null ? school.getUuid().toString() : null,
                        school.getName(),
                        school.getRegistrationNumber(),
                        school.isActive(),
                        school.isSuspended(),
                        school.getCreatedAt(),
                        userCounts.getOrDefault(school.getId(), 0L),
                        null))
                .collect(Collectors.toList());
    }

    private List<RecentAuditLogDto> recentAuditLogs() {
        List<com.edumanage.audit.entity.AuditLog> logs = auditLogRepository.findLatest(PageRequest.of(0, RECENT_AUDIT_LOGS_LIMIT));
        return logs.stream()
                .map(log -> new RecentAuditLogDto(
                        log.getId(),
                        log.getActorUserId(),
                        log.getActorName(),
                        log.getActorRole(),
                        log.getSchoolId(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getResult(),
                        log.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
