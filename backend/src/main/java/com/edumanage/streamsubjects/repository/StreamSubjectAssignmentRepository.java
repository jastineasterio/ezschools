package com.edumanage.streamsubjects.repository;

import com.edumanage.streamsubjects.entity.StreamSubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamSubjectAssignmentRepository extends JpaRepository<StreamSubjectAssignment, Long> {
    List<StreamSubjectAssignment> findBySchoolIdAndClassIdAndStreamIdAndAcademicYearId(
            Long schoolId, Long classId, Long streamId, Long academicYearId);
    List<StreamSubjectAssignment> findBySchoolIdAndClassIdAndAcademicYearIdAndSubjectId(
            Long schoolId, Long classId, Long academicYearId, Long subjectId);

    List<StreamSubjectAssignment> findBySchoolIdAndStreamIdAndAcademicYearId(
            Long schoolId, Long streamId, Long academicYearId);

    boolean existsBySchoolIdAndClassIdAndStreamIdAndAcademicYearIdAndSubjectId(
            Long schoolId, Long classId, Long streamId, Long academicYearId, Long subjectId);

    void deleteBySchoolIdAndClassIdAndStreamIdAndAcademicYearId(
            Long schoolId, Long classId, Long streamId, Long academicYearId);
}
