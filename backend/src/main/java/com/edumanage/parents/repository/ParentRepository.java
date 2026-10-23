package com.edumanage.parents.repository;

import com.edumanage.parents.entity.Parent;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
    Page<Parent> findBySchoolId(Long schoolId, Pageable pageable);
    Page<Parent> findBySchoolIdAndDeletedAtIsNull(Long schoolId, Pageable pageable);
    Optional<Parent> findBySchoolIdAndId(Long schoolId, Long id);
    Optional<Parent> findByUserId(Long userId);
    Optional<Parent> findFirstBySchoolIdAndEmailIgnoreCase(Long schoolId, String email);
}
