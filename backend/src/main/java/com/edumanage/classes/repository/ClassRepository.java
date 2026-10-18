package com.edumanage.classes.repository;

import com.edumanage.classes.entity.Class;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {
    Page<Class> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Class> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
}
