package com.edumanage.library.repository;

import com.edumanage.library.entity.Fine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    Page<Fine> findBySchoolId(Long schoolId, Pageable pageable);
    List<Fine> findBySchoolIdAndMemberId(Long schoolId, Long memberId);
    List<Fine> findBySchoolIdAndStatus(Long schoolId, String status);
    Optional<Fine> findBySchoolIdAndId(Long schoolId, Long id);
    List<Fine> findBySchoolIdAndBorrowingId(Long schoolId, Long borrowingId);
}
