package com.edumanage.schools.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.subscriptions.repository.SubscriptionRepository;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolServiceTest {

    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SubscriptionRepository subscriptionRepository;
    private SchoolService schoolService;

    @BeforeEach
    void setUp() {
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        schoolService = new SchoolService(
                schoolRepository,
                userRepository,
                subscriptionRepository,
                mock(PackageRepository.class),
                mock(AuditLogService.class));
    }

    @Test
    void mapsGroupedUserCountsToTheMatchingSchoolAndDefaultsMissingCountsToZero() {
        School schoolWithUsers = new School();
        schoolWithUsers.setId(11L);
        School schoolWithoutUsers = new School();
        schoolWithoutUsers.setId(12L);
        when(schoolRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(schoolWithUsers, schoolWithoutUsers)));
        when(userRepository.countSchoolUsersBySchoolIds(List.of(11L, 12L)))
                .thenReturn(List.of(userCount(11L, 4L)));
        when(subscriptionRepository.findBySchoolId(11L)).thenReturn(List.of());
        when(subscriptionRepository.findBySchoolId(12L)).thenReturn(List.of());

        var response = schoolService.listSchools(PageRequest.of(0, 10));

        assertEquals(4L, response.getContent().get(0).getUserCount());
        assertEquals(0L, response.getContent().get(1).getUserCount());
        verify(userRepository).countSchoolUsersBySchoolIds(List.of(11L, 12L));
    }

    @Test
    void returnsZeroUsersForAnEmptySchoolPage() {
        when(schoolRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        var response = schoolService.listSchools(PageRequest.of(0, 10));

        assertEquals(0, response.getContent().size());
        verify(userRepository, org.mockito.Mockito.never()).countSchoolUsersBySchoolIds(any());
    }

    private UserRepository.SchoolUserCount userCount(Long schoolId, Long count) {
        return new UserRepository.SchoolUserCount() {
            @Override
            public Long getSchoolId() {
                return schoolId;
            }

            @Override
            public Long getUserCount() {
                return count;
            }
        };
    }
}
