package com.edumanage.fees.repository;

import com.edumanage.fees.entity.Payment;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Payment> findBySchoolIdAndId(Long schoolId, Long id);
    List<Payment> findBySchoolIdAndStudentId(Long schoolId, Long studentId);
    List<Payment> findBySchoolIdAndFeeAssignmentId(Long schoolId, Long feeAssignmentId);
    boolean existsBySchoolIdAndReceiptNumber(Long schoolId, String receiptNumber);
    boolean existsBySchoolIdAndReferenceNumber(Long schoolId, String referenceNumber);
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.schoolId = :schoolId AND p.deletedAt IS NULL")
    BigDecimal sumAmountBySchoolId(@Param("schoolId") Long schoolId);
}
