package com.edumanage.classes.repository;

import com.edumanage.classes.entity.Class;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {
    Page<Class> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Class> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);

    @Query("SELECT c FROM Class c WHERE (:schoolId IS NULL OR c.schoolId = :schoolId) "
            + "AND c.deletedAt IS NULL AND (CAST(:search AS string) IS NULL "
            + "OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(COALESCE(c.code, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Class> search(@Param("schoolId") Long schoolId, @Param("search") String search, Pageable pageable);
}
