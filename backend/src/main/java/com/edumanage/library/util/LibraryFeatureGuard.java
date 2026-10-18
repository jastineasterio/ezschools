package com.edumanage.library.util;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.packages.entity.PackageFeature;
import com.edumanage.packages.repository.PackageFeatureRepository;
import com.edumanage.subscriptions.entity.Subscription;
import com.edumanage.subscriptions.repository.SubscriptionRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LibraryFeatureGuard {

    private static PackageFeatureRepository packageFeatureRepository;
    private static SubscriptionRepository subscriptionRepository;

    public LibraryFeatureGuard(PackageFeatureRepository packageFeatureRepository,
                               SubscriptionRepository subscriptionRepository) {
        LibraryFeatureGuard.packageFeatureRepository = packageFeatureRepository;
        LibraryFeatureGuard.subscriptionRepository = subscriptionRepository;
    }

    public static void checkLibraryFeature(Long schoolId) {
        if (schoolId == null) {
            throw new ForbiddenException("Access denied");
        }

        List<Subscription> activeSubscriptions = subscriptionRepository.findBySchoolId(schoolId).stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .toList();

        if (activeSubscriptions.isEmpty()) {
            throw new ForbiddenException("No active subscription found");
        }

        boolean hasLibraryFeature = activeSubscriptions.stream().anyMatch(sub -> {
            List<PackageFeature> features = packageFeatureRepository.findByPkgId(sub.getPackageId());
            return features.stream().anyMatch(f -> "LIBRARY".equals(f.getFeatureKey()) && f.isEnabled());
        });

        if (!hasLibraryFeature) {
            throw new ForbiddenException("Library feature is not available in your subscription package");
        }
    }
}
