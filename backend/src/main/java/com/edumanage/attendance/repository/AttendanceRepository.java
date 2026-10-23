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
    Page<Attendance> findBySchoolIdAndDeletedAtIsNull(Long schoolId, Pageable pageable);
    Page<Attendance> findBySchoolIdAndClassIdAndDeletedAtIsNull(Long schoolId, Long classId, Pageable pageable);
    Page<Attendance> findBySchoolIdAndAttendanceDateAndDeletedAtIsNull(Long schoolId, LocalDate attendanceDate, Pageable pageable);
    Page<Attendance> findBySchoolIdAndClassIdAndAttendanceDateAndDeletedAtIsNull(Long schoolId, Long classId, LocalDate attendanceDate, Pageable pageable);
    Page<Attendance> findByClassIdAndDeletedAtIsNull(Long classId, Pageable pageable);
    Page<Attendance> findByAttendanceDateAndDeletedAtIsNull(LocalDate attendanceDate, Pageable pageable);
    Page<Attendance> findByClassIdAndAttendanceDateAndDeletedAtIsNull(Long classId, LocalDate attendanceDate, Pageable pageable);
    Page<Attendance> findByDeletedAtIsNull(Pageable pageable);
    List<Attendance> findBySchoolIdAndAttendanceDate(Long schoolId, LocalDate attendanceDate);
    List<Attendance> findByStudentIdAndSchoolId(Long studentId, Long schoolId);
    List<Attendance> findByClassIdAndAcademicYearIdAndAttendanceDate(Long classId, Long academicYearId, LocalDate attendanceDate);
    List<Attendance> findBySchoolIdAndClassIdAndAcademicYearIdAndAttendanceDateAndDeletedAtIsNull(
            Long schoolId, Long classId, Long academicYearId, LocalDate attendanceDate);
    List<Attendance> findByTeacherIdAndAttendanceDate(Long teacherId, LocalDate attendanceDate);
    boolean existsBySchoolIdAndStudentIdAndClassIdAndAttendanceDateAndSubjectId(Long schoolId, Long studentId, Long classId, LocalDate attendanceDate, Long subjectId);
    long countBySchoolIdAndStatusAndAttendanceDateBetween(Long schoolId, String status, LocalDate start, LocalDate end);
}
