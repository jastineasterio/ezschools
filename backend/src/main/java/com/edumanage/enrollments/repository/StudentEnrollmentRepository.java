package com.edumanage.enrollments.repository;

import com.edumanage.enrollments.entity.StudentEnrollment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {
    Page<StudentEnrollment> findBySchoolId(Long schoolId, Pageable pageable);
    List<StudentEnrollment> findByStudentIdAndSchoolId(Long studentId, Long schoolId);
    Optional<StudentEnrollment> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
    List<StudentEnrollment> findByClassIdAndAcademicYearId(Long classId, Long academicYearId);
    List<StudentEnrollment> findBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId);
    boolean existsByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
}
