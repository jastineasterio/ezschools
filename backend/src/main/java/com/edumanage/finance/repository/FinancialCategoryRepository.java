package com.edumanage.finance.repository;

import com.edumanage.finance.entity.FinancialCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, Long> {
    List<FinancialCategory> findBySchoolIdAndCategoryType(Long schoolId, String categoryType);
    Page<FinancialCategory> findBySchoolIdAndCategoryType(Long schoolId, String categoryType, Pageable pageable);
    Optional<FinancialCategory> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
}
