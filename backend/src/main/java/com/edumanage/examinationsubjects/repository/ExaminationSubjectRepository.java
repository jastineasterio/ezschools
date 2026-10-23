package com.edumanage.examinationsubjects.repository;

import com.edumanage.examinationsubjects.entity.ExaminationSubject;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExaminationSubjectRepository extends JpaRepository<ExaminationSubject, Long> {
    Page<ExaminationSubject> findByExaminationId(Long examinationId, Pageable pageable);
    List<ExaminationSubject> findByExaminationId(Long examinationId);
    Optional<ExaminationSubject> findByExaminationIdAndSubjectIdAndClassIdAndStreamId(Long examinationId, Long subjectId, Long classId, Long streamId);

    @Query("""
            select distinct new com.edumanage.teacherworkspace.dto.TeacherUpcomingExaminationDto(
                es.id, e.id, e.name, e.startDate, e.endDate, e.status,
                s.name, c.name, st.name, e.academicYearId, es.maximumMarks)
            from ExaminationSubject es
            join es.examination e
            join es.subject s
            join es.clazz c
            left join es.stream st,
            TeacherAssignment ta
            where ta.teacherId = :teacherId and ta.schoolId = :schoolId
              and ta.deletedAt is null
              and es.deletedAt is null and e.deletedAt is null
              and e.schoolId = :schoolId and upper(e.status) <> 'DRAFT'
              and coalesce(e.endDate, e.startDate) >= :today
              and es.examinationId = e.id
              and ta.classId = es.classId
              and ta.subjectId = es.subjectId
              and ta.academicYearId = e.academicYearId
              and (ta.streamId is null or ta.streamId = es.streamId)
            order by e.startDate asc, e.name asc
            """)
    List<com.edumanage.teacherworkspace.dto.TeacherUpcomingExaminationDto> findUpcomingForTeacher(
            @Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId,
            @Param("today") java.time.LocalDate today);
}
