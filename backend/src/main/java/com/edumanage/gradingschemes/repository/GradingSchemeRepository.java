package com.edumanage.gradingschemes.repository;

import com.edumanage.gradingschemes.entity.GradingScheme;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradingSchemeRepository extends JpaRepository<GradingScheme, Long> {
    Page<GradingScheme> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<GradingScheme> findBySchoolIdAndIsDefaultTrue(Long schoolId);
    Optional<GradingScheme> findBySchoolIdAndId(Long schoolId, Long id);
}
