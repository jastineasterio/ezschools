package com.edumanage.teachertimetables.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.teachertimetables.dto.CreateTeacherTimetableRequest;
import com.edumanage.teachertimetables.dto.TeacherTimetableDto;
import com.edumanage.teachertimetables.entity.TeacherTimetable;
import com.edumanage.teachertimetables.repository.TeacherTimetableRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherTimetableService {
    private final TeacherTimetableRepository timetableRepository;
    private final TeacherAssignmentRepository assignmentRepository;

    public TeacherTimetableService(TeacherTimetableRepository timetableRepository,
                                   TeacherAssignmentRepository assignmentRepository) {
        this.timetableRepository = timetableRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherTimetableDto> listForSchool() {
        Long schoolId = TenantContext.requireTenantId();
        return timetableRepository.findBySchoolIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(schoolId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public TeacherTimetableDto create(CreateTeacherTimetableRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        TeacherAssignment assignment = assignmentRepository.findById(request.getTeacherAssignmentId())
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment not found"));
        if (!schoolId.equals(assignment.getSchoolId())) {
            throw new ForbiddenException("Teacher assignment does not belong to this school");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Lesson end time must be after its start time");
        }
        if (timetableRepository.countTeacherTimeConflicts(schoolId, assignment.getTeacherId(),
                request.getDayOfWeek(), request.getStartTime(), request.getEndTime()) > 0) {
            throw new ConflictException("Teacher already has a lesson during this time");
        }

        TeacherTimetable timetable = new TeacherTimetable();
        timetable.setSchoolId(schoolId);
        timetable.setTeacherAssignmentId(assignment.getId());
        timetable.setDayOfWeek(request.getDayOfWeek());
        timetable.setStartTime(request.getStartTime());
        timetable.setEndTime(request.getEndTime());
        timetable.setRoom(request.getRoom());
        return toDto(timetableRepository.save(timetable), assignment);
    }

    @Transactional
    public void delete(Long id) {
        Long schoolId = TenantContext.requireTenantId();
        TeacherTimetable timetable = timetableRepository.findById(id)
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found"));
        if (!schoolId.equals(timetable.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
        timetable.setDeletedAt(java.time.LocalDateTime.now());
        timetableRepository.save(timetable);
    }

    private TeacherTimetableDto toDto(TeacherTimetable timetable) {
        return toDto(timetable, timetable.getTeacherAssignment());
    }

    private TeacherTimetableDto toDto(TeacherTimetable timetable, TeacherAssignment assignment) {
        return new TeacherTimetableDto(
                timetable.getId(), timetable.getTeacherAssignmentId(), timetable.getDayOfWeek(),
                timetable.getStartTime(), timetable.getEndTime(), timetable.getRoom(),
                assignment.getClazz().getName(),
                assignment.getStream() == null ? null : assignment.getStream().getName(),
                assignment.getSubject().getName());
    }
}
