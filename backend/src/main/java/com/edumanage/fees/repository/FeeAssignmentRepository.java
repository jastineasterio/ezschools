package com.edumanage.fees.repository;

import com.edumanage.fees.entity.FeeAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeAssignmentRepository extends JpaRepository<FeeAssignment, Long> {

    interface FeeStructureTotals {
        Long getFeeStructureId();

        Long getAssignments();

        BigDecimal getBilled();

        BigDecimal getCollected();

        BigDecimal getOutstanding();
    }

    Page<FeeAssignment> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<FeeAssignment> findBySchoolIdAndId(Long schoolId, Long id);
    List<FeeAssignment> findBySchoolIdAndStudentId(Long schoolId, Long studentId);
    List<FeeAssignment> findBySchoolIdAndStudentIdAndStatus(Long schoolId, Long studentId, String status);
    List<FeeAssignment> findBySchoolIdAndStatus(Long schoolId, String status);
    boolean existsBySchoolIdAndStudentIdAndFeeStructureId(Long schoolId, Long studentId, Long feeStructureId);
    @Query("SELECT SUM(f.balance) FROM FeeAssignment f WHERE f.schoolId = :schoolId AND f.deletedAt IS NULL")
    BigDecimal sumBalanceBySchoolId(@Param("schoolId") Long schoolId);

    @Query("SELECT SUM(f.amountDue) FROM FeeAssignment f WHERE f.schoolId = :schoolId AND f.deletedAt IS NULL")
    BigDecimal sumAmountDueBySchoolId(@Param("schoolId") Long schoolId);

    @Query("SELECT SUM(f.amountPaid) FROM FeeAssignment f WHERE f.schoolId = :schoolId AND f.deletedAt IS NULL")
    BigDecimal sumAmountPaidBySchoolId(@Param("schoolId") Long schoolId);

    long countBySchoolIdAndBalanceGreaterThan(Long schoolId, BigDecimal balance);

    @Query("SELECT SUM(f.amountDue) FROM FeeAssignment f WHERE f.schoolId = :schoolId AND f.deletedAt IS NULL AND f.assignedAt >= :fromDate")
    BigDecimal sumAmountDueSince(@Param("schoolId") Long schoolId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT SUM(f.amountPaid) FROM FeeAssignment f WHERE f.schoolId = :schoolId AND f.deletedAt IS NULL AND f.assignedAt >= :fromDate")
    BigDecimal sumAmountPaidSince(@Param("schoolId") Long schoolId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT SUM(f.balance) FROM FeeAssignment f WHERE f.schoolId = :schoolId AND f.deletedAt IS NULL AND f.assignedAt >= :fromDate")
    BigDecimal sumBalanceSince(@Param("schoolId") Long schoolId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT COUNT(DISTINCT f.studentId) FROM FeeAssignment f WHERE f.schoolId = :schoolId "
            + "AND f.deletedAt IS NULL AND f.balance > 0 AND f.assignedAt >= :fromDate")
    long countDistinctStudentsWithBalanceSince(@Param("schoolId") Long schoolId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT f.feeStructureId AS feeStructureId, COUNT(f) AS assignments, SUM(f.amountDue) AS billed, "
            + "SUM(f.amountPaid) AS collected, SUM(f.balance) AS outstanding FROM FeeAssignment f "
            + "WHERE f.schoolId = :schoolId AND f.deletedAt IS NULL GROUP BY f.feeStructureId")
    List<FeeStructureTotals> summariseByFeeStructure(@Param("schoolId") Long schoolId);
}
