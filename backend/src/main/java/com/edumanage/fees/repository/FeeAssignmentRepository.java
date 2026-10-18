package com.edumanage.fees.repository;

import com.edumanage.fees.entity.FeeAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeAssignmentRepository extends JpaRepository<FeeAssignment, Long> {
    Page<FeeAssignment> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<FeeAssignment> findBySchoolIdAndId(Long schoolId, Long id);
    List<FeeAssignment> findBySchoolIdAndStudentId(Long schoolId, Long studentId);
    List<FeeAssignment> findBySchoolIdAndStudentIdAndStatus(Long schoolId, Long studentId, String status);
    boolean existsBySchoolIdAndStudentIdAndFeeStructureId(Long schoolId, Long studentId, Long feeStructureId);
    @Query("SELECT SUM(f.balance) FROM FeeAssignment f WHERE f.schoolId = :schoolId AND f.deletedAt IS NULL")
    BigDecimal sumBalanceBySchoolId(@Param("schoolId") Long schoolId);
}
