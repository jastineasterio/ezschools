package com.edumanage.departments.repository;

import com.edumanage.departments.entity.Department;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Page<Department> findBySchoolId(Long schoolId, Pageable pageable);
    List<Department> findBySchoolIdAndHeadOfDepartmentId(Long schoolId, Long headOfDepartmentId);
    Optional<Department> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
    boolean existsBySchoolIdAndCode(Long schoolId, String code);

    @Query("SELECT d FROM Department d WHERE (:schoolId IS NULL OR d.schoolId = :schoolId) "
            + "AND d.deletedAt IS NULL AND (CAST(:search AS string) IS NULL "
            + "OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(COALESCE(d.code, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Department> search(@Param("schoolId") Long schoolId, @Param("search") String search, Pageable pageable);
}
