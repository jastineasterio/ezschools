package com.edumanage.attendance.repository;

import com.edumanage.attendance.entity.Attendance;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Page<Attendance> findBySchoolId(Long schoolId, Pageable pageable);
    List<Attendance> findBySchoolIdAndAttendanceDate(Long schoolId, LocalDate attendanceDate);
    List<Attendance> findByStudentIdAndSchoolId(Long studentId, Long schoolId);
    List<Attendance> findByClassIdAndAcademicYearIdAndAttendanceDate(Long classId, Long academicYearId, LocalDate attendanceDate);
    List<Attendance> findByTeacherIdAndAttendanceDate(Long teacherId, LocalDate attendanceDate);
    boolean existsBySchoolIdAndStudentIdAndClassIdAndAttendanceDateAndSubjectId(Long schoolId, Long studentId, Long classId, LocalDate attendanceDate, Long subjectId);
    long countBySchoolIdAndStatusAndAttendanceDateBetween(Long schoolId, String status, LocalDate start, LocalDate end);
}
