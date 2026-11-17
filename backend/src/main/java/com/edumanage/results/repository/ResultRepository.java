package com.edumanage.results.repository;

import com.edumanage.results.dto.ResultAnalyticsRow;
import com.edumanage.results.entity.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    @EntityGraph(attributePaths = {
            "student",
            "examinationSubject",
            "examinationSubject.examination",
            "examinationSubject.subject",
            "examinationSubject.clazz",
            "examinationSubject.stream",
            "teacher"
    })
    Page<Result> findBySchoolId(Long schoolId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "student",
            "examinationSubject",
            "examinationSubject.examination",
            "examinationSubject.subject",
            "examinationSubject.clazz",
            "examinationSubject.stream",
            "teacher"
    })
    Page<Result> findByStudentIdAndSchoolId(Long studentId, Long schoolId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "student",
            "examinationSubject",
            "examinationSubject.examination",
            "examinationSubject.subject",
            "examinationSubject.clazz",
            "examinationSubject.stream",
            "teacher"
    })
    Page<Result> findByStudentIdAndSchoolIdAndStatusAndDeletedAtIsNull(
            Long studentId, Long schoolId, String status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "student",
            "examinationSubject",
            "examinationSubject.examination",
            "examinationSubject.subject",
            "examinationSubject.clazz",
            "examinationSubject.stream",
            "teacher"
    })
    Page<Result> findByExaminationIdAndSchoolId(Long examinationId, Long schoolId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "student",
            "examinationSubject",
            "examinationSubject.examination",
            "examinationSubject.subject",
            "examinationSubject.clazz",
            "examinationSubject.stream",
            "teacher"
    })
    Optional<Result> findById(Long id);

    Optional<Result> findByStudentIdAndExaminationSubjectId(Long studentId, Long examinationSubjectId);
    List<Result> findByExaminationSubjectIdAndDeletedAtIsNull(Long examinationSubjectId);
    List<Result> findByExaminationIdAndStatus(Long examinationId, String status);
    List<Result> findByTeacherIdAndSchoolIdAndDeletedAtIsNull(Long teacherId, Long schoolId);
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

    @EntityGraph(attributePaths = {
            "student",
            "examinationSubject",
            "examinationSubject.examination",
            "examinationSubject.subject",
            "examinationSubject.clazz",
            "examinationSubject.stream",
            "teacher"
    })
    @Query("""
            SELECT r FROM Result r
            JOIN r.examinationSubject es
            JOIN es.examination e
            WHERE r.schoolId = :schoolId AND r.deletedAt IS NULL
              AND es.deletedAt IS NULL AND e.deletedAt IS NULL
              AND e.schoolId = :schoolId
              AND r.examinationId = :examinationId
              AND es.classId = :classId
              AND (:streamId IS NULL OR es.streamId = :streamId)
              AND r.status IN :statuses
            ORDER BY r.createdAt DESC
            """)
    List<Result> findByExaminationAndClassAndStreamAndStatus(
            @Param("schoolId") Long schoolId,
            @Param("examinationId") Long examinationId,
            @Param("classId") Long classId,
            @Param("streamId") Long streamId,
            @Param("statuses") List<String> statuses);

    @EntityGraph(attributePaths = {
            "student",
            "examinationSubject",
            "examinationSubject.examination",
            "examinationSubject.subject",
            "examinationSubject.clazz",
            "examinationSubject.stream",
            "teacher"
    })
    @Query("""
            SELECT r FROM Result r
            JOIN r.examinationSubject es
            JOIN r.examination e
            WHERE r.schoolId = :schoolId AND r.deletedAt IS NULL
              AND es.deletedAt IS NULL AND e.deletedAt IS NULL
              AND e.schoolId = :schoolId
              AND (:examinationId IS NULL OR r.examinationId = :examinationId)
              AND (:classId IS NULL OR es.classId = :classId)
              AND (:streamId IS NULL OR es.streamId = :streamId)
              AND (:subjectId IS NULL OR es.subjectId = :subjectId)
            ORDER BY r.createdAt DESC
            """)
    Page<Result> findByFilters(
            @Param("schoolId") Long schoolId,
            @Param("examinationId") Long examinationId,
            @Param("classId") Long classId,
            @Param("streamId") Long streamId,
            @Param("subjectId") Long subjectId,
            Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT
                r.id AS resultId,
                r.student_id AS studentId,
                sp.admission_number AS admissionNumber,
                (sp.first_name || ' ' || sp.last_name) AS studentName,
                es.class_id AS classId,
                c.name AS className,
                es.stream_id AS streamId,
                st.name AS streamName,
                es.subject_id AS subjectId,
                sub.name AS subjectName,
                r.examination_id AS examinationId,
                e.name AS examinationName,
                e.academic_year_id AS academicYearId,
                r.marks_obtained AS marksObtained,
                r.percentage AS percentage,
                r.grade AS grade,
                r.status AS status,
                r.submitted_at AS submittedAt,
                es.pass_mark AS passMark
            FROM results r
            JOIN student_profiles sp ON sp.id = r.student_id AND sp.deleted_at IS NULL
            JOIN examination_subjects es ON es.id = r.examination_subject_id AND es.deleted_at IS NULL
            JOIN examinations e ON e.id = es.examination_id AND e.deleted_at IS NULL
            JOIN classes c ON c.id = es.class_id AND c.deleted_at IS NULL
            LEFT JOIN streams st ON st.id = es.stream_id AND st.deleted_at IS NULL
            JOIN subjects sub ON sub.id = es.subject_id AND sub.deleted_at IS NULL
            WHERE r.school_id = :schoolId AND r.deleted_at IS NULL
              AND (:academicYearId IS NULL OR e.academic_year_id = :academicYearId)
              AND (:examinationId IS NULL OR r.examination_id = :examinationId)
              AND (:classId IS NULL OR es.class_id = :classId)
              AND (:streamId IS NULL OR es.stream_id = :streamId)
              AND (:subjectId IS NULL OR es.subject_id = :subjectId)
              AND (:startDate IS NULL OR e.start_date >= :startDate)
              AND (:endDate IS NULL OR e.end_date <= :endDate)
              AND r.marks_obtained IS NOT NULL
            ORDER BY r.created_at DESC
            """)
    List<ResultAnalyticsRow> findResultsForAnalytics(
            @Param("schoolId") Long schoolId,
            @Param("academicYearId") Long academicYearId,
            @Param("examinationId") Long examinationId,
            @Param("classId") Long classId,
            @Param("streamId") Long streamId,
            @Param("subjectId") Long subjectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
