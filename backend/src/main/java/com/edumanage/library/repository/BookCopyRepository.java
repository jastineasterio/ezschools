package com.edumanage.library.repository;

import com.edumanage.library.entity.BookCopy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findBySchoolIdAndBookId(Long schoolId, Long bookId);
    Page<BookCopy> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<BookCopy> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndBarcode(Long schoolId, String barcode);
    boolean existsBySchoolIdAndBarcodeAndIdNot(Long schoolId, String barcode, Long id);
    boolean existsBySchoolIdAndBookIdAndStatus(Long schoolId, Long bookId, String status);
    long countBySchoolIdAndBookIdAndStatus(Long schoolId, Long bookId, String status);
    List<BookCopy> findBySchoolIdAndStatus(Long schoolId, String status);
}
