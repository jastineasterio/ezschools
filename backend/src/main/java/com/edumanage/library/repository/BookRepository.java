package com.edumanage.library.repository;

import com.edumanage.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findBySchoolId(Long schoolId, Pageable pageable);
    List<Book> findBySchoolIdAndCategoryId(Long schoolId, Long categoryId);
    Optional<Book> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsBySchoolIdAndIsbn(Long schoolId, String isbn);
    boolean existsBySchoolIdAndIsbnAndIdNot(Long schoolId, String isbn, Long id);
}
