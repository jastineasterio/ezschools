package com.edumanage.platform.analytics.service;

import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.platform.analytics.dto.UserOverviewDto;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.schools.repository.SchoolRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformUserAnalyticsServiceTest {

    @Test
    void overviewMapsRepositoryCounts() {
        EntityManager em = mock(EntityManager.class);
        Query countQuery = mock(Query.class);
        Query tupleQuery = mock(Query.class);

        Tuple counts = mock(Tuple.class);
        when(counts.get("total_users")).thenReturn(100L);
        when(counts.get("active_users")).thenReturn(70L);
        when(counts.get("inactive_users")).thenReturn(25L);
        when(counts.get("deleted_users")).thenReturn(5L);

        when(em.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(tupleQuery);
        when(tupleQuery.getSingleResult()).thenReturn(counts);
        when(countQuery.getSingleResult()).thenReturn(20L, 10L);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(em.createNativeQuery(anyString())).thenReturn(countQuery);

        PlatformUserAnalyticsService service = new PlatformUserAnalyticsService(
                em, mock(RoleRepository.class), mock(PackageRepository.class), mock(SchoolRepository.class));

        UserOverviewDto dto = service.getOverview(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertEquals(100L, dto.getTotalUsers());
        assertEquals(70L, dto.getActiveUsers());
        assertEquals(25L, dto.getInactiveUsers());
        assertEquals(5L, dto.getDeletedUsers());
        assertEquals(20L, dto.getTotalSchools());
        assertEquals(10L, dto.getNewUsersPeriod());
        assertNotNull(dto.getPeriodStart());
        assertNotNull(dto.getPeriodEnd());
    }

    @Test
    void averageUsersPerSchoolIsZeroWhenNoSchools() {
        EntityManager em = mock(EntityManager.class);
        Query tupleQuery = mock(Query.class);
        Query countQuery = mock(Query.class);

        Tuple counts = mock(Tuple.class);
        when(counts.get("total_users")).thenReturn(50L);
        when(counts.get("active_users")).thenReturn(30L);
        when(counts.get("inactive_users")).thenReturn(20L);
        when(counts.get("deleted_users")).thenReturn(0L);
        when(tupleQuery.getSingleResult()).thenReturn(counts);
        when(em.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(tupleQuery);
        when(countQuery.getSingleResult()).thenReturn(0L, 0L);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(em.createNativeQuery(anyString())).thenReturn(countQuery);

        PlatformUserAnalyticsService service = new PlatformUserAnalyticsService(
                em, mock(RoleRepository.class), mock(PackageRepository.class), mock(SchoolRepository.class));

        UserOverviewDto dto = service.getOverview(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertEquals(0.0, dto.getAverageUsersPerSchool());
    }

    @Test
    void growthPercentIsNullWithoutHistoricalData() {
        EntityManager em = mock(EntityManager.class);
        Query tupleQuery = mock(Query.class);
        Query countQuery = mock(Query.class);

        Tuple counts = mock(Tuple.class);
        when(counts.get("total_users")).thenReturn(10L);
        when(counts.get("active_users")).thenReturn(6L);
        when(counts.get("inactive_users")).thenReturn(4L);
        when(counts.get("deleted_users")).thenReturn(0L);
        when(tupleQuery.getSingleResult()).thenReturn(counts);
        when(em.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(tupleQuery);
        when(countQuery.getSingleResult()).thenReturn(0L, 10L, 0L);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(em.createNativeQuery(anyString())).thenReturn(countQuery);

        PlatformUserAnalyticsService service = new PlatformUserAnalyticsService(
                em, mock(RoleRepository.class), mock(PackageRepository.class), mock(SchoolRepository.class));

        UserOverviewDto dto = service.getOverview(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertNull(dto.getGrowthPercent());
    }

    @Test
    void getOverviewValidatesRange() {
        EntityManager em = mock(EntityManager.class);
        PlatformUserAnalyticsService service = new PlatformUserAnalyticsService(
                em, mock(RoleRepository.class), mock(PackageRepository.class), mock(SchoolRepository.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.getOverview(null, LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> service.getOverview(LocalDateTime.now(), LocalDateTime.now()));
    }
}
