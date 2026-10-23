package com.edumanage.academicyears.repository;

import com.edumanage.academicyears.entity.AcademicYear;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(Long schoolId);
    Optional<AcademicYear> findBySchoolIdAndIsCurrentTrueAndDeletedAtIsNull(Long schoolId);
    boolean existsBySchoolIdAndName(Long schoolId, String name);

    @Query("""
            SELECT CASE WHEN COUNT(ay) > 0 THEN TRUE ELSE FALSE END
            FROM AcademicYear ay
            WHERE ay.schoolId = :schoolId
              AND ay.deletedAt IS NULL
              AND ay.startDate <= :endDate
              AND ay.endDate >= :startDate
              AND (:excludeId IS NULL OR ay.id <> :excludeId)
            """)
    boolean existsOverlapping(@Param("schoolId") Long schoolId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("excludeId") Long excludeId);

    @Query("SELECT ay FROM AcademicYear ay WHERE (:schoolId IS NULL OR ay.schoolId = :schoolId) "
            + "AND ay.deletedAt IS NULL AND (CAST(:search AS string) IS NULL "
            + "OR LOWER(ay.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
            + "ORDER BY ay.startDate DESC")
    Page<AcademicYear> search(@Param("schoolId") Long schoolId, @Param("search") String search, Pageable pageable);
}
