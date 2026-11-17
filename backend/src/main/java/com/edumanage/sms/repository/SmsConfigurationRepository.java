package com.edumanage.sms.repository;

import com.edumanage.sms.entity.SmsConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SmsConfigurationRepository extends JpaRepository<SmsConfiguration, Long> {
    Optional<SmsConfiguration> findBySchoolIdAndIsActiveAndDeletedAtIsNull(Long schoolId, boolean isActive);
    Optional<SmsConfiguration> findBySchoolIdAndProviderAndDeletedAtIsNull(Long schoolId, String provider);
    Page<SmsConfiguration> findBySchoolIdAndDeletedAtIsNull(Long schoolId, Pageable pageable);
    boolean existsBySchoolIdAndDeletedAtIsNull(Long schoolId);
    boolean existsBySchoolIdAndProviderAndDeletedAtIsNull(Long schoolId, String provider);
    Page<SmsConfiguration> findByDeletedAtIsNull(Pageable pageable);
}
