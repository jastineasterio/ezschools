package com.edumanage.results.repository;

import com.edumanage.results.entity.GradeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradeSnapshotRepository extends JpaRepository<GradeSnapshot, Long> {
    Optional<GradeSnapshot> findByResultId(Long resultId);
}
