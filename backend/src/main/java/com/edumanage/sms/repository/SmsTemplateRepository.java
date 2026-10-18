package com.edumanage.sms.repository;

import com.edumanage.sms.entity.SmsTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {
    Page<SmsTemplate> findBySchoolIdAndIsActive(Long schoolId, boolean isActive, Pageable pageable);
    List<SmsTemplate> findBySchoolIdAndIsActive(Long schoolId, boolean isActive);
    Optional<SmsTemplate> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
}
