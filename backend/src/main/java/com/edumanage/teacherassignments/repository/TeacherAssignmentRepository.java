package com.edumanage.teacherassignments.repository;

import com.edumanage.teacherassignments.entity.TeacherAssignment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    Page<TeacherAssignment> findBySchoolId(Long schoolId, Pageable pageable);
    List<TeacherAssignment> findByTeacherIdAndSchoolId(Long teacherId, Long schoolId);
    List<TeacherAssignment> findByTeacherIdAndAcademicYearIdAndSchoolId(Long teacherId, Long academicYearId, Long schoolId);
    boolean existsByTeacherIdAndSubjectIdAndClassIdAndAcademicYearId(Long teacherId, Long subjectId, Long classId, Long academicYearId);
    void deleteByTeacherIdAndAcademicYearId(Long teacherId, Long academicYearId);
}
