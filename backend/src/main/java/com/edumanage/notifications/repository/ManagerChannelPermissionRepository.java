package com.edumanage.notifications.repository;

import com.edumanage.notifications.entity.ManagerChannelPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerChannelPermissionRepository extends JpaRepository<ManagerChannelPermission, Long> {
    Optional<ManagerChannelPermission> findByManagerUserId(Long managerUserId);
}
