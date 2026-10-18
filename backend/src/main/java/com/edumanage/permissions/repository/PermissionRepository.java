package com.edumanage.permissions.repository;

import com.edumanage.permissions.entity.Permission;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    boolean existsByName(String name);
}
