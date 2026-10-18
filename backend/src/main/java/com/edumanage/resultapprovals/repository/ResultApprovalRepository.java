package com.edumanage.resultapprovals.repository;

import com.edumanage.resultapprovals.entity.ResultApproval;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultApprovalRepository extends JpaRepository<ResultApproval, Long> {
    Page<ResultApproval> findByResultId(Long resultId, Pageable pageable);
    List<ResultApproval> findByResultIdOrderByCreatedAtDesc(Long resultId);
}
