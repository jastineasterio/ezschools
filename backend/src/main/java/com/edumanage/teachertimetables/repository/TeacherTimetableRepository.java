package com.edumanage.teachertimetables.repository;

import com.edumanage.teachertimetables.dto.TeacherTimetableDto;
import com.edumanage.teachertimetables.entity.TeacherTimetable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
            select count(t)
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

    @Query("""
            select new com.edumanage.teachertimetables.dto.TeacherTimetableDto(
                    tt.id, tt.teacherAssignmentId, tt.dayOfWeek, tt.startTime, tt.endTime, tt.room,
                    c.name, s.name, sub.name)
            from TeacherTimetable tt
            join tt.teacherAssignment ta
            join ta.clazz c
            left join ta.stream s
            join ta.subject sub
            where tt.schoolId = :schoolId and tt.deletedAt is null
            order by tt.dayOfWeek asc, tt.startTime asc
            """)
    List<TeacherTimetableDto> findDtoBySchoolId(@Param("schoolId") Long schoolId);

    @Modifying
    @Query("delete from TeacherTimetable t where t.schoolId = :schoolId")
    void deleteBySchoolId(@Param("schoolId") Long schoolId);
}
