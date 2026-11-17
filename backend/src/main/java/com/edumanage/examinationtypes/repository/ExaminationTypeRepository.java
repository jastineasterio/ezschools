package com.edumanage.examinationtypes.repository;

import com.edumanage.examinationtypes.entity.ExaminationType;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExaminationTypeRepository extends JpaRepository<ExaminationType, Long> {
    Page<ExaminationType> findBySchoolId(Long schoolId, Pageable pageable);

    Page<ExaminationType> findBySchoolIdAndDeletedAtIsNull(Long schoolId, Pageable pageable);

    Page<ExaminationType> findBySchoolIdAndActiveTrueAndDeletedAtIsNull(Long schoolId, Pageable pageable);

    Page<ExaminationType> findBySchoolIdAndDeletedAtIsNullAndNameContainingIgnoreCase(Long schoolId, String name, Pageable pageable);

    Optional<ExaminationType> findBySchoolIdAndId(Long schoolId, Long id);

    boolean existsBySchoolIdAndName(Long schoolId, String name);

    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);

    boolean existsBySchoolIdAndNameIgnoreCaseAndIdNot(Long schoolId, String name, Long id);

    @Query("SELECT e FROM ExaminationType e WHERE e.schoolId = :schoolId AND e.deletedAt IS NULL AND e.id = :id")
    Optional<ExaminationType> findNonDeletedBySchoolIdAndId(@Param("schoolId") Long schoolId, @Param("id") Long id);

    long countBySchoolIdAndDeletedAtIsNull(Long schoolId);
}
