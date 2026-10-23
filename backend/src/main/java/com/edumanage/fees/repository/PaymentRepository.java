package com.edumanage.fees.repository;

import com.edumanage.fees.entity.Payment;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    interface PaymentDateSummary {
        LocalDate getPaymentDate();

        Long getTotal();

        BigDecimal getAmount();
    }

    interface PaymentMethodSummary {
        String getPaymentMethod();

        Long getTotal();

        BigDecimal getAmount();
    }

    Page<Payment> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Payment> findBySchoolIdAndId(Long schoolId, Long id);
    List<Payment> findBySchoolIdAndStudentId(Long schoolId, Long studentId);
    List<Payment> findBySchoolIdAndFeeAssignmentId(Long schoolId, Long feeAssignmentId);
    boolean existsBySchoolIdAndReceiptNumber(Long schoolId, String receiptNumber);
    boolean existsBySchoolIdAndReferenceNumber(Long schoolId, String referenceNumber);
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL")
    BigDecimal sumAmountBySchoolId(@Param("schoolId") Long schoolId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedAmountBySchoolId(@Param("schoolId") Long schoolId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL AND p.status <> 'COMPLETED'")
    BigDecimal sumUnverifiedAmountBySchoolId(@Param("schoolId") Long schoolId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL")
    long countAllBySchoolId(@Param("schoolId") Long schoolId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL "
            + "AND p.status = 'COMPLETED' AND p.paymentDate >= :fromDate")
    BigDecimal sumCompletedAmountSince(@Param("schoolId") Long schoolId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL AND p.paymentDate >= :fromDate")
    long countSince(@Param("schoolId") Long schoolId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL AND p.status <> 'COMPLETED'")
    long countUnverifiedBySchoolId(@Param("schoolId") Long schoolId);

    @Query("SELECT p FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL ORDER BY p.paymentDate DESC, p.id DESC")
    List<Payment> findRecentBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL AND p.status <> 'COMPLETED' ORDER BY p.paymentDate DESC, p.id DESC")
    List<Payment> findUnverifiedBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("SELECT p.paymentDate AS paymentDate, COUNT(p) AS total, SUM(p.amount) AS amount FROM Payment p "
            + "WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL AND p.status = 'COMPLETED' "
            + "AND p.paymentDate >= :fromDate GROUP BY p.paymentDate ORDER BY p.paymentDate DESC")
    List<PaymentDateSummary> summariseByPaymentDate(@Param("schoolId") Long schoolId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT p.paymentMethod AS paymentMethod, COUNT(p) AS total, SUM(p.amount) AS amount FROM Payment p "
            + "WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL AND p.status = 'COMPLETED' "
            + "GROUP BY p.paymentMethod ORDER BY SUM(p.amount) DESC")
    List<PaymentMethodSummary> summariseByPaymentMethod(@Param("schoolId") Long schoolId);
}
