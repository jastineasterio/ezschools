package com.edumanage.examinations.repository;

import com.edumanage.examinations.entity.Examination;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    Page<Examination> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Examination> findBySchoolIdAndId(Long schoolId, Long id);
    Page<Examination> findBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId, Pageable pageable);
    long countBySchoolIdAndDeletedAtIsNull(Long schoolId);
}
