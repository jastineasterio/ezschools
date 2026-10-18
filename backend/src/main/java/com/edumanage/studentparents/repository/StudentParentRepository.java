package com.edumanage.studentparents.repository;

import com.edumanage.studentparents.entity.StudentParent;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentParentRepository extends JpaRepository<StudentParent, Long> {
    Optional<StudentParent> findByStudentIdAndParentIdAndRelationshipType(Long studentId, Long parentId, String relationshipType);
    List<StudentParent> findByParentId(Long parentId);
    List<StudentParent> findByStudentId(Long studentId);
    boolean existsByStudentIdAndParentId(Long studentId, Long parentId);
}
