package com.edumanage.sms.repository;

import com.edumanage.sms.entity.SmsConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsConfigurationRepository extends JpaRepository<SmsConfiguration, Long> {
    Optional<SmsConfiguration> findBySchoolIdAndIsActive(Long schoolId, boolean isActive);
    Optional<SmsConfiguration> findBySchoolIdAndProvider(Long schoolId, String provider);
    Optional<SmsConfiguration> findBySchoolIdAndProviderAndIsActive(Long schoolId, String provider, boolean isActive);
    boolean existsBySchoolId(Long schoolId);
}
