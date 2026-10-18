package com.edumanage.library.repository;

import com.edumanage.library.entity.Borrowing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    Page<Borrowing> findBySchoolId(Long schoolId, Pageable pageable);
    List<Borrowing> findBySchoolIdAndMemberId(Long schoolId, Long memberId);
    Optional<Borrowing> findBySchoolIdAndId(Long schoolId, Long id);
    List<Borrowing> findBySchoolIdAndBookCopyId(Long schoolId, Long bookCopyId);
    List<Borrowing> findBySchoolIdAndStatus(Long schoolId, String status);
    List<Borrowing> findBySchoolIdAndDueDateBeforeAndStatus(Long schoolId, LocalDate dueDate, String status);
    Optional<Borrowing> findBySchoolIdAndBookCopyIdAndStatus(Long schoolId, Long bookCopyId, String status);
    boolean existsBySchoolIdAndBookCopyIdAndStatus(Long schoolId, Long bookCopyId, String status);
    long countBySchoolIdAndMemberIdAndStatus(Long schoolId, Long memberId, String status);
}
