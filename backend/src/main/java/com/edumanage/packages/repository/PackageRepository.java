package com.edumanage.packages.repository;

import com.edumanage.packages.entity.Package;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {
    Optional<Package> findByUuid(UUID uuid);
    Optional<Package> findByName(String name);
    boolean existsByName(String name);
    Page<Package> findByIsActive(boolean isActive, Pageable pageable);
    Optional<Package> findByIsDefaultTrue();
}
