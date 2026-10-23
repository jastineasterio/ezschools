package com.edumanage.subjects.repository;

import com.edumanage.subjects.entity.Subject;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Page<Subject> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Subject> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
    boolean existsBySchoolIdAndCode(Long schoolId, String code);

    @Query("SELECT s FROM Subject s WHERE (:schoolId IS NULL OR s.schoolId = :schoolId) "
            + "AND s.deletedAt IS NULL AND (CAST(:search AS string) IS NULL "
            + "OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(COALESCE(s.code, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Subject> search(@Param("schoolId") Long schoolId, @Param("search") String search, Pageable pageable);
}
