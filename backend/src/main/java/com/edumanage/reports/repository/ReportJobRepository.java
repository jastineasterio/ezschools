package com.edumanage.reports.repository;

import com.edumanage.reports.entity.ReportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {
    Page<ReportJob> findBySchoolId(Long schoolId, Pageable pageable);
    Page<ReportJob> findBySchoolIdAndRequestedBy(Long schoolId, Long requestedBy, Pageable pageable);
    Page<ReportJob> findByRequestedBy(Long requestedBy, Pageable pageable);

    @Query("SELECT j FROM ReportJob j WHERE j.id = :id AND j.schoolId = :schoolId AND j.deletedAt IS NULL")
    Optional<ReportJob> findByIdAndSchoolId(Long id, Long schoolId);
}

