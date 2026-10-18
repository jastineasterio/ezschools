package com.edumanage.gradingschemeranges.repository;

import com.edumanage.gradingschemeranges.entity.GradingSchemeRange;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradingSchemeRangeRepository extends JpaRepository<GradingSchemeRange, Long> {
    List<GradingSchemeRange> findBySchemeIdOrderByMinMarkDesc(Long schemeId);
    void deleteBySchemeId(Long schemeId);
}
