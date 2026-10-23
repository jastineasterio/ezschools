package com.edumanage.teacherassignments.repository;

import com.edumanage.teacherassignments.entity.TeacherAssignment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    Page<TeacherAssignment> findBySchoolId(Long schoolId, Pageable pageable);
    @EntityGraph(attributePaths = {"teacher", "subject", "clazz", "stream", "academicYear"})
    Page<TeacherAssignment> findBySchoolIdAndDeletedAtIsNull(Long schoolId, Pageable pageable);
    List<TeacherAssignment> findByTeacherIdAndSchoolId(Long teacherId, Long schoolId);
    @EntityGraph(attributePaths = {"teacher", "subject", "clazz", "stream", "academicYear"})
    List<TeacherAssignment> findByTeacherIdAndSchoolIdAndDeletedAtIsNull(Long teacherId, Long schoolId);
    List<TeacherAssignment> findByTeacherIdAndAcademicYearIdAndSchoolId(Long teacherId, Long academicYearId, Long schoolId);
    boolean existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
            Long teacherId, Long schoolId, Long subjectId, Long classId, Long streamId, Long academicYearId);
    boolean existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdIsNullAndAcademicYearIdAndDeletedAtIsNull(
            Long teacherId, Long schoolId, Long subjectId, Long classId, Long academicYearId);
    boolean existsByTeacherIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
            Long teacherId, Long subjectId, Long classId, Long streamId, Long academicYearId);
    boolean existsByTeacherIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndIdNotAndDeletedAtIsNull(
            Long teacherId, Long subjectId, Long classId, Long streamId, Long academicYearId, Long id);
    void deleteByTeacherIdAndAcademicYearId(Long teacherId, Long academicYearId);
}
