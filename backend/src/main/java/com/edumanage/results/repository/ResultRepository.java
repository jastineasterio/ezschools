package com.edumanage.results.repository;

import com.edumanage.results.entity.Result;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    Page<Result> findBySchoolId(Long schoolId, Pageable pageable);
    Page<Result> findByStudentIdAndSchoolId(Long studentId, Long schoolId, Pageable pageable);
    Page<Result> findByExaminationIdAndSchoolId(Long examinationId, Long schoolId, Pageable pageable);
    Optional<Result> findByStudentIdAndExaminationSubjectId(Long studentId, Long examinationSubjectId);
    List<Result> findByExaminationIdAndStatus(Long examinationId, String status);
    long countBySchoolIdAndStatus(Long schoolId, String status);
    long countBySchoolIdAndStatusAndDeletedAtIsNull(Long schoolId, String status);
    @Query("SELECT AVG(r.marksObtained) FROM Result r WHERE r.schoolId = :schoolId AND r.deletedAt IS NULL")
    Double findAverageScoreBySchoolId(@Param("schoolId") Long schoolId);
}
