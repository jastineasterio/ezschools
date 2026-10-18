package com.edumanage.subjects.repository;

import com.edumanage.subjects.entity.Subject;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Page<Subject> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Subject> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
    boolean existsBySchoolIdAndCode(Long schoolId, String code);
}
