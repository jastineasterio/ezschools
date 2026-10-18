package com.edumanage.schools.repository;

import com.edumanage.schools.entity.School;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByUuid(UUID uuid);
    boolean existsByRegistrationNumber(String registrationNumber);
    boolean existsByEmail(String email);
    Page<School> findAll(Pageable pageable);
    Page<School> findByIsActive(boolean isActive, Pageable pageable);
    long countByIsActive(boolean isActive);
    long countByIsSuspended(boolean isSuspended);
}
