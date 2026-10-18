package com.edumanage.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateAttendanceRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Academic year ID is required")
    private Long academicYearId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotBlank(message = "Status is required")
    private String status;

    private Long streamId;

    private Long subjectId;

    private Long teacherId;

    private String remarks;

    public CreateAttendanceRequest() {
    }

    public CreateAttendanceRequest(Long studentId, Long classId, Long academicYearId, LocalDate attendanceDate, String status, Long streamId, Long subjectId, Long teacherId, String remarks) {
        this.studentId = studentId;
        this.classId = classId;
        this.academicYearId = academicYearId;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.streamId = streamId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.remarks = remarks;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
