package com.edumanage.sms.repository;

import com.edumanage.sms.entity.SmsPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsPurchaseRepository extends JpaRepository<SmsPurchase, Long> {

    Optional<SmsPurchase> findByPurchaseId(String purchaseId);

    Page<SmsPurchase> findBySchoolId(Long schoolId, Pageable pageable);

    Page<SmsPurchase> findBySchoolIdAndStatus(Long schoolId, String status, Pageable pageable);

    boolean existsByPurchaseId(String purchaseId);
}
