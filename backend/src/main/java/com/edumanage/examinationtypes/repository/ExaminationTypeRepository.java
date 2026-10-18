package com.edumanage.examinationtypes.repository;

import com.edumanage.examinationtypes.entity.ExaminationType;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExaminationTypeRepository extends JpaRepository<ExaminationType, Long> {
    Page<ExaminationType> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<ExaminationType> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
}
