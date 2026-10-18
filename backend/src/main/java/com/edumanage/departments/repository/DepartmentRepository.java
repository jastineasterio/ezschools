package com.edumanage.departments.repository;

import com.edumanage.departments.entity.Department;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Page<Department> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Department> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
    boolean existsBySchoolIdAndCode(Long schoolId, String code);
}
