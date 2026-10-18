package com.edumanage.branding.repository;

import com.edumanage.branding.entity.SchoolBranding;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolBrandingRepository extends JpaRepository<SchoolBranding, Long> {
    Optional<SchoolBranding> findBySchoolId(Long schoolId);
    boolean existsBySchoolId(Long schoolId);
}
