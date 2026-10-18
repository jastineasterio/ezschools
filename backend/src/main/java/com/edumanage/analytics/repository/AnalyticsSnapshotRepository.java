package com.edumanage.analytics.repository;

import com.edumanage.analytics.entity.AnalyticsSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, Long> {
    Page<AnalyticsSnapshot> findBySchoolId(Long schoolId, Pageable pageable);
    List<AnalyticsSnapshot> findBySchoolIdAndSnapshotDateBetween(Long schoolId, java.time.LocalDate start, java.time.LocalDate end);
    List<AnalyticsSnapshot> findBySchoolIdAndMetricType(Long schoolId, String metricType);
}
