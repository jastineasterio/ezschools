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
    Page<Result> findByStudentIdAndSchoolIdAndStatusAndDeletedAtIsNull(
            Long studentId, Long schoolId, String status, Pageable pageable);
    Page<Result> findByExaminationIdAndSchoolId(Long examinationId, Long schoolId, Pageable pageable);
    Optional<Result> findByStudentIdAndExaminationSubjectId(Long studentId, Long examinationSubjectId);
    List<Result> findByExaminationSubjectIdAndDeletedAtIsNull(Long examinationSubjectId);
    List<Result> findByExaminationIdAndStatus(Long examinationId, String status);
    long countBySchoolIdAndStatus(Long schoolId, String status);
    long countBySchoolIdAndStatusAndDeletedAtIsNull(Long schoolId, String status);
    @Query("SELECT AVG(r.marksObtained) FROM Result r WHERE r.schoolId = :schoolId AND r.deletedAt IS NULL")
    Double findAverageScoreBySchoolId(@Param("schoolId") Long schoolId);

    @Query("""
            select r from Result r
            join r.examinationSubject es
            join r.examination e
            where r.schoolId = :schoolId and e.schoolId = :schoolId
              and r.status = 'PUBLISHED' and r.deletedAt is null
              and es.deletedAt is null and e.deletedAt is null
              and es.examinationId = e.id
              and e.academicYearId = :academicYearId
              and es.classId = :classId
              and ((:streamId is null and es.streamId is null) or es.streamId = :streamId)
              and es.subjectId = :subjectId
              and r.marksObtained is not null and r.percentage is not null
              and r.percentage >= 0 and r.percentage <= 100
              and r.marksObtained >= 0 and r.marksObtained <= es.maximumMarks
            """)
    List<Result> findPublishedForCertificateScope(@Param("schoolId") Long schoolId,
                                                  @Param("academicYearId") Long academicYearId,
                                                  @Param("classId") Long classId,
                                                  @Param("streamId") Long streamId,
                                                  @Param("subjectId") Long subjectId);
}
