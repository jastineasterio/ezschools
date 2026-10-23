package com.edumanage.boarding.repository;

import com.edumanage.boarding.entity.BoardingAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardingAttendanceRepository extends JpaRepository<BoardingAttendance, Long> {
    Page<BoardingAttendance> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<BoardingAttendance> findBySchoolIdAndId(Long schoolId, Long id);
    List<BoardingAttendance> findBySchoolIdAndDormitoryIdAndAttendanceDate(Long schoolId, Long dormitoryId, LocalDate date);
    List<BoardingAttendance> findBySchoolIdAndAttendanceDate(Long schoolId, LocalDate date);
    List<BoardingAttendance> findBySchoolIdAndStudentIdAndAttendanceDate(Long schoolId, Long studentId, LocalDate date);
    List<BoardingAttendance> findBySchoolIdAndDormitoryIdAndAttendanceDateBetween(Long schoolId, Long dormitoryId, LocalDate start, LocalDate end);
    boolean existsBySchoolIdAndStudentIdAndDormitoryIdAndAttendanceDate(Long schoolId, Long studentId, Long dormitoryId, LocalDate date);
}
