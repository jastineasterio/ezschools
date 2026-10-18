package com.edumanage.sms.repository;

import com.edumanage.sms.entity.SmsMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long> {
    Page<SmsMessage> findBySchoolId(Long schoolId, Pageable pageable);
    List<SmsMessage> findBySchoolIdAndStatus(Long schoolId, String status);
    Page<SmsMessage> findBySchoolIdAndStatus(Long schoolId, String status, Pageable pageable);
    Optional<SmsMessage> findBySchoolIdAndId(Long schoolId, Long id);
    Page<SmsMessage> findBySchoolIdAndTemplateId(Long schoolId, Long templateId, Pageable pageable);
    long countBySchoolIdAndStatus(Long schoolId, String status);
}
