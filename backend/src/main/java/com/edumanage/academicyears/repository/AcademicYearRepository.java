package com.edumanage.academicyears.repository;

import com.edumanage.academicyears.entity.AcademicYear;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(Long schoolId);
    Optional<AcademicYear> findBySchoolIdAndIsCurrentTrue(Long schoolId);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
}
