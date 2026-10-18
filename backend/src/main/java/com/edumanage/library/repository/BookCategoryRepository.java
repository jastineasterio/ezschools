package com.edumanage.library.repository;

import com.edumanage.library.entity.BookCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {
    Page<BookCategory> findBySchoolId(Long schoolId, Pageable pageable);
    List<BookCategory> findBySchoolIdAndStatus(Long schoolId, String status);
    Optional<BookCategory> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);
    boolean existsBySchoolIdAndNameIgnoreCaseAndIdNot(Long schoolId, String name, Long id);
}
