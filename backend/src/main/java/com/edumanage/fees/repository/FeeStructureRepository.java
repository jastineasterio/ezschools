package com.edumanage.fees.repository;

import com.edumanage.fees.entity.FeeStructure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    Page<FeeStructure> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<FeeStructure> findBySchoolIdAndId(Long schoolId, Long id);
    List<FeeStructure> findBySchoolIdAndClassId(Long schoolId, Long classId);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
}
