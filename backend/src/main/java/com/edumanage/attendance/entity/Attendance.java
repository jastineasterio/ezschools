package com.edumanage.attendance.entity;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.classes.entity.Class;
import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.streams.entity.Stream;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "student_id", "class_id", "stream_id", "attendance_date", "subject_id"})
})
public class Attendance extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "stream_id")
    private Long streamId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private Class clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_id", insertable = false, updatable = false)
    private Stream stream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", insertable = false, updatable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private User teacher;

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
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

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public void setStudent(StudentProfile student) {
        this.student = student;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Stream getStream() {
        return stream;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }
}
