package com.edumanage.resultsnotifications.repository;

import com.edumanage.resultsnotifications.entity.ResultNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResultNotificationSettingRepository extends JpaRepository<ResultNotificationSetting, Long> {
    Optional<ResultNotificationSetting> findBySchoolId(Long schoolId);
}
