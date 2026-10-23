package com.edumanage.teachertimetables.repository;

import com.edumanage.teachertimetables.entity.TeacherTimetable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeacherTimetableRepository extends JpaRepository<TeacherTimetable, Long> {
    @EntityGraph(attributePaths = {"teacherAssignment", "teacherAssignment.subject",
            "teacherAssignment.clazz", "teacherAssignment.stream"})
    List<TeacherTimetable> findBySchoolIdAndTeacherAssignmentIdInAndDayOfWeekAndDeletedAtIsNullOrderByStartTime(
            Long schoolId, List<Long> assignmentIds, int dayOfWeek);

    @EntityGraph(attributePaths = {"teacherAssignment", "teacherAssignment.subject",
            "teacherAssignment.clazz", "teacherAssignment.stream"})
    List<TeacherTimetable> findBySchoolIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(Long schoolId);

    @Query("""
            select count(t) > 0
            from TeacherTimetable t
            join t.teacherAssignment scheduledAssignment
            where t.schoolId = :schoolId and t.dayOfWeek = :dayOfWeek
              and t.deletedAt is null
              and scheduledAssignment.teacherId = :teacherId
              and :startTime < t.endTime and :endTime > t.startTime
            """)
    long countTeacherTimeConflicts(@Param("schoolId") Long schoolId,
                                   @Param("teacherId") Long teacherId,
                                   @Param("dayOfWeek") int dayOfWeek,
                                   @Param("startTime") java.time.LocalTime startTime,
                                   @Param("endTime") java.time.LocalTime endTime);
}
