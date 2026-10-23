package com.edumanage.subscriptions.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.packages.entity.Package;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.subscriptions.dto.AssignPackageRequest;
import com.edumanage.subscriptions.entity.Subscription;
import com.edumanage.subscriptions.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {

    private SubscriptionRepository subscriptionRepository;
    private PackageRepository packageRepository;
    private SchoolRepository schoolRepository;
    private SubscriptionService subscriptionService;
    private School school;
    private Package pkg;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        packageRepository = mock(PackageRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        subscriptionService = new SubscriptionService(
                subscriptionRepository,
                packageRepository,
                schoolRepository,
                mock(AuditLogService.class));

        school = new School();
        school.setId(10L);
        school.setName("Example School");
        pkg = new Package();
        pkg.setId(20L);
        pkg.setName("annual");
        pkg.setDisplayName("Annual");
        pkg.setPrice(new BigDecimal("120.00"));
        pkg.setBillingPeriod("YEARLY");
        pkg.setActive(true);
    }

    @Test
    void assignsExistingPackageToSchoolAndPersistsSubscription() {
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(school));
        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));
        when(packageRepository.findById(20L)).thenReturn(Optional.of(pkg));
        when(subscriptionRepository.findBySchoolId(10L)).thenReturn(List.of());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription saved = invocation.getArgument(0);
            saved.setId(30L);
            return saved;
        });

        var response = subscriptionService.assignPackage(10L, new AssignPackageRequest(20L, null));

        var persisted = org.mockito.ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(persisted.capture());
        assertEquals(10L, persisted.getValue().getSchoolId());
        assertEquals(20L, persisted.getValue().getPackageId());
        assertEquals("ACTIVE", persisted.getValue().getStatus());
        assertEquals("PENDING", persisted.getValue().getPaymentStatus());
        assertEquals("YEARLY", persisted.getValue().getBillingPeriod());
        assertEquals(pkg.getPrice(), persisted.getValue().getPrice());
        assertEquals(1, ChronoUnit.YEARS.between(
                persisted.getValue().getStartDate(), persisted.getValue().getEndDate()));
        assertEquals("Example School", response.getSchoolName());
        assertEquals("Annual", response.getPackageName());
        assertEquals(30L, response.getId());
    }

    @Test
    void rejectsNonexistentSchool() {
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.assignPackage(10L, new AssignPackageRequest(20L, null)));

        verify(packageRepository, never()).findById(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void rejectsNonexistentPackage() {
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(school));
        when(packageRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.assignPackage(10L, new AssignPackageRequest(20L, null)));

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void rejectsInactivePackage() {
        pkg.setActive(false);
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(school));
        when(packageRepository.findById(20L)).thenReturn(Optional.of(pkg));

        assertThrows(BadRequestException.class,
                () -> subscriptionService.assignPackage(10L, new AssignPackageRequest(20L, null)));

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void transitionsTrialToPaidPackageAndPreservesPreviousPrice() {
        var trialSubscription = new Subscription();
        trialSubscription.setId(15L);
        trialSubscription.setSchoolId(10L);
        trialSubscription.setPackageId(19L);
        trialSubscription.setStatus("TRIAL");
        trialSubscription.setPrice(new BigDecimal("15.00"));
        trialSubscription.setStartDate(LocalDateTime.now().minusDays(10));
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(school));
        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));
        when(packageRepository.findById(20L)).thenReturn(Optional.of(pkg));
        when(subscriptionRepository.findBySchoolId(10L)).thenReturn(List.of(trialSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = subscriptionService.assignPackage(10L, new AssignPackageRequest(20L, null));

        assertEquals("CANCELLED", trialSubscription.getStatus());
        assertNotNull(trialSubscription.getCancelledAt());
        assertEquals(new BigDecimal("15.00"), trialSubscription.getPrice());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("PENDING", response.getPaymentStatus());
        verify(subscriptionRepository).saveAll(List.of(trialSubscription));
    }

    @Test
    void doesNotReplaceAnUnexpiredPaidSubscription() {
        var paidSubscription = new Subscription();
        paidSubscription.setId(15L);
        paidSubscription.setSchoolId(10L);
        paidSubscription.setStatus("ACTIVE");
        paidSubscription.setEndDate(LocalDateTime.now().plusDays(10));
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(school));
        when(packageRepository.findById(20L)).thenReturn(Optional.of(pkg));
        when(subscriptionRepository.findBySchoolId(10L)).thenReturn(List.of(paidSubscription));

        assertThrows(ConflictException.class,
                () -> subscriptionService.assignPackage(10L, new AssignPackageRequest(20L, null)));

        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(subscriptionRepository, never()).saveAll(any());
    }

    @Test
    void rejectsUnsupportedStartDates() {
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(school));
        when(packageRepository.findById(20L)).thenReturn(Optional.of(pkg));

        assertThrows(BadRequestException.class,
                () -> subscriptionService.assignPackage(10L,
                        new AssignPackageRequest(20L, null, LocalDate.now().plusDays(1))));

        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void rejectsUnsupportedBillingPeriodsBeforeChangingExistingSubscription() {
        pkg.setBillingPeriod("WEEKLY");
        var trialSubscription = new Subscription();
        trialSubscription.setId(15L);
        trialSubscription.setStatus("TRIAL");
        trialSubscription.setStartDate(LocalDateTime.now().minusDays(2));
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(school));
        when(packageRepository.findById(20L)).thenReturn(Optional.of(pkg));
        when(subscriptionRepository.findBySchoolId(10L)).thenReturn(List.of(trialSubscription));

        assertThrows(BadRequestException.class,
                () -> subscriptionService.assignPackage(10L, new AssignPackageRequest(20L, null)));

        assertEquals("TRIAL", trialSubscription.getStatus());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(subscriptionRepository, never()).saveAll(any());
    }

    @Test
    void doesNotCreateAnotherActiveSubscriptionWhenActivatingAReplacement() {
        var activeSubscription = new Subscription();
        activeSubscription.setId(15L);
        activeSubscription.setSchoolId(10L);
        activeSubscription.setStatus("ACTIVE");
        activeSubscription.setEndDate(LocalDateTime.now().minusDays(1));
        var trialSubscription = new Subscription();
        trialSubscription.setId(16L);
        trialSubscription.setSchoolId(10L);
        trialSubscription.setPackageId(20L);
        trialSubscription.setStatus("TRIAL");
        when(subscriptionRepository.findById(16L)).thenReturn(Optional.of(trialSubscription));
        when(schoolRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(school));
        when(subscriptionRepository.findBySchoolIdAndStatus(10L, "ACTIVE"))
                .thenReturn(List.of(activeSubscription));
        when(packageRepository.findById(20L)).thenReturn(Optional.of(pkg));

        var response = subscriptionService.activateSubscription(16L);

        assertEquals("CANCELLED", activeSubscription.getStatus());
        assertNotNull(activeSubscription.getCancelledAt());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("PENDING", response.getPaymentStatus());
        verify(subscriptionRepository).saveAll(List.of(activeSubscription));
    }
}
