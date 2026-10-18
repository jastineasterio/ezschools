package com.edumanage.platform.service;

import com.edumanage.platform.dto.PlatformDashboardDto;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.subscriptions.repository.SubscriptionRepository;
import com.edumanage.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlatformDashboardService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public PlatformDashboardService(SchoolRepository schoolRepository, UserRepository userRepository, SubscriptionRepository subscriptionRepository) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public PlatformDashboardDto getDashboard() {
        var totalSchools = schoolRepository.countByIsActive(true) + schoolRepository.countByIsActive(false);
        var activeSchools = schoolRepository.countByIsActive(true);
        var suspendedSchools = schoolRepository.countByIsSuspended(true);
        var totalUsers = userRepository.count();
        var activeSubscriptions = subscriptionRepository.countByStatus("ACTIVE");

        var packageDistribution = subscriptionRepository.findAll().stream()
                .collect(Collectors.groupingBy(s -> "Package " + s.getPackageId(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new PlatformDashboardDto.PackageDistribution(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new PlatformDashboardDto(totalSchools, activeSchools, suspendedSchools, totalUsers, activeSubscriptions, packageDistribution, List.of(), List.of());
    }
}
