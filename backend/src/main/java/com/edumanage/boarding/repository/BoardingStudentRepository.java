package com.edumanage.boarding.repository;

import com.edumanage.boarding.entity.BoardingStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardingStudentRepository extends JpaRepository<BoardingStudent, Long> {
    Page<BoardingStudent> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<BoardingStudent> findBySchoolIdAndId(Long schoolId, Long id);
    List<BoardingStudent> findBySchoolIdAndStatus(Long schoolId, String status);
    Optional<BoardingStudent> findBySchoolIdAndStudentIdAndStatus(Long schoolId, Long studentId, String status);
    boolean existsBySchoolIdAndStudentIdAndStatus(Long schoolId, Long studentId, String status);
    List<BoardingStudent> findBySchoolIdAndDormitoryId(Long schoolId, Long dormitoryId);
}
