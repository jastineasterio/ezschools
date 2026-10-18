package com.edumanage.finance.repository;

import com.edumanage.finance.entity.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Page<FinancialTransaction> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<FinancialTransaction> findBySchoolIdAndId(Long schoolId, Long id);
    List<FinancialTransaction> findBySchoolIdAndCategoryId(Long schoolId, Long categoryId);
    List<FinancialTransaction> findBySchoolIdAndTransactionType(Long schoolId, String transactionType);
    List<FinancialTransaction> findBySchoolIdAndPaymentId(Long schoolId, Long paymentId);
}
