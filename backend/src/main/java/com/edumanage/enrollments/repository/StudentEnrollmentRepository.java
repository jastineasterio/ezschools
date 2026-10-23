package com.edumanage.enrollments.repository;

import com.edumanage.enrollments.entity.StudentEnrollment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {
    Page<StudentEnrollment> findBySchoolId(Long schoolId, Pageable pageable);
    @EntityGraph(attributePaths = {"student", "academicYear", "clazz", "stream"})
    Page<StudentEnrollment> findBySchoolIdAndDeletedAtIsNull(Long schoolId, Pageable pageable);
    @EntityGraph(attributePaths = {"student", "stream"})
    List<StudentEnrollment> findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
            Long schoolId, Long classId, Long academicYearId, String status);
    @Query("""
            select e.classId as classId, e.streamId as streamId,
                   e.academicYearId as academicYearId, count(e) as studentCount
            from StudentEnrollment e
            where e.schoolId = :schoolId and e.classId in :classIds
              and e.academicYearId in :academicYearIds
              and e.status = 'ENROLLED' and e.deletedAt is null
            group by e.classId, e.streamId, e.academicYearId
            """)
    List<EnrollmentCount> countEnrolledByClassAndStream(@Param("schoolId") Long schoolId,
                                                         @Param("classIds") List<Long> classIds,
                                                         @Param("academicYearIds") List<Long> academicYearIds);
    List<StudentEnrollment> findByStudentIdAndSchoolId(Long studentId, Long schoolId);
    Optional<StudentEnrollment> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
    Optional<StudentEnrollment> findByStudentIdAndAcademicYearIdAndIdNot(Long studentId, Long academicYearId, Long id);
    List<StudentEnrollment> findByClassIdAndAcademicYearId(Long classId, Long academicYearId);
    List<StudentEnrollment> findBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId);
    List<StudentEnrollment> findBySchoolIdAndClassId(Long schoolId, Long classId);
    boolean existsByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    interface EnrollmentCount {
        Long getClassId();
        Long getStreamId();
        Long getAcademicYearId();
        Long getStudentCount();
    }
}
