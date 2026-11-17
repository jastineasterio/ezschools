package com.edumanage.teacherworkspace.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teacherassignments.service.TeacherAssignmentService;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinationtypes.entity.ExaminationType;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.examinationsubjects.entity.ExaminationSubject;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.results.dto.ResultDto;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.security.CurrentUserService;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.teacherworkspace.dto.TeacherResultDtos;
import com.edumanage.tenant.TenantContext;
import com.edumanage.notifications.repository.NotificationRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherWorkspaceResultServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private TeacherAssignmentService assignmentService;
    @Mock private SchoolRepository schoolRepository;
    @Mock private TeacherAssignmentRepository assignmentRepository;
    @Mock private com.edumanage.streams.repository.StreamRepository streamRepository;
    @Mock private com.edumanage.subjects.repository.SubjectRepository subjectRepository;
    @Mock private com.edumanage.academicyears.repository.AcademicYearRepository academicYearRepository;
    @Mock private ExaminationSubjectRepository examinationSubjectRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private com.edumanage.results.service.ResultService resultService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private com.edumanage.enrollments.repository.StudentEnrollmentRepository enrollmentRepository;
    @InjectMocks private TeacherWorkspaceService service;

    private final Long schoolId = 7L;
    private final Long teacherId = 1L;
    private final Long classId = 30L;
    private final Long streamId = 40L;
    private final Long subjectId = 50L;
    private final Long academicYearId = 20L;

    private User teacher;
    private School school;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(schoolId);

        teacher = new User();
        teacher.setId(teacherId);
        teacher.setEmail("teacher@school.test");
        teacher.setSchoolId(schoolId);

        school = new School();
        school.setId(schoolId);
        school.setName("Demo School");
        school.setTimeZone("UTC");

        when(currentUserService.requireCurrentUser()).thenReturn(teacher);
        when(currentUserService.hasRole(teacher, "TEACHER")).thenReturn(true);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private List<TeacherAssignmentDto> assignments(Long classId, Long streamId, Long subjectId,
                                                   Long academicYearId, String className, String subjectName) {
        TeacherAssignmentDto a = new TeacherAssignmentDto();
        a.setId(1L);
        a.setTeacherId(teacherId);
        a.setSchoolId(schoolId);
        a.setSubjectId(subjectId);
        a.setSubjectName(subjectName);
        a.setClassId(classId);
        a.setClassName(className);
        a.setStreamId(streamId);
        a.setStreamName(streamId == null ? null : "Stream " + streamId);
        a.setAcademicYearId(academicYearId);
        a.setAcademicYearName("Year 2024");
        a.setStudentCount(0L);
        return List.of(a);
    }

    @Test
    void resultClassesReturnsDistinctAssignedClasses() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(
                assignments(classId, streamId, subjectId, academicYearId, "Grade 10", "Math"));

        List<TeacherResultDtos.ClassOption> classes = service.resultClasses();

        assertEquals(1, classes.size());
        assertEquals(classId, classes.get(0).id());
        assertEquals("Grade 10", classes.get(0).name());
        assertEquals(academicYearId, classes.get(0).academicYearId());
    }

    @Test
    void resultStreamsReturnsStreamsForAssignedClass() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(
                assignments(classId, streamId, subjectId, academicYearId, "Grade 10", "Math"));
        com.edumanage.streams.entity.Stream stream = new com.edumanage.streams.entity.Stream();
        stream.setId(streamId);
        stream.setName("East");
        stream.setSchoolId(schoolId);
        stream.setClassId(classId);
        stream.setDeletedAt(null);
        when(streamRepository.findByClassIdAndSchoolId(classId, schoolId)).thenReturn(List.of(stream));

        List<TeacherResultDtos.StreamOption> streams = service.resultStreams(classId);

        assertEquals(1, streams.size());
        assertEquals(streamId, streams.get(0).id());
        assertEquals("East", streams.get(0).name());
    }

    @Test
    void resultStreamsRejectsUnassignedClass() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(List.of());
        assertThrows(ForbiddenException.class, () -> service.resultStreams(classId));
    }

    @Test
    void resultStudentsReturnsEnrolledStudentsInStream() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(
                assignments(classId, streamId, subjectId, academicYearId, "Grade 10", "Math"));
        StudentProfile student = new StudentProfile();
        student.setId(10L);
        student.setSchoolId(schoolId);
        student.setFirstName("Jane");
        student.setLastName("Doe");
        student.setAdmissionNumber("ADM001");
        student.setDeletedAt(null);
        com.edumanage.enrollments.entity.StudentEnrollment enrollment = new com.edumanage.enrollments.entity.StudentEnrollment();
        enrollment.setStudentId(10L);
        enrollment.setSchoolId(schoolId);
        enrollment.setClassId(classId);
        enrollment.setStreamId(streamId);
        enrollment.setAcademicYearId(academicYearId);
        enrollment.setStatus("ENROLLED");
        enrollment.setDeletedAt(null);
        enrollment.setStudent(student);
        when(enrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                schoolId, classId, academicYearId, "ENROLLED")).thenReturn(List.of(enrollment));

        List<TeacherResultDtos.StudentOption> students = service.resultStudents(classId, streamId, academicYearId);

        assertEquals(1, students.size());
        assertEquals(10L, students.get(0).id());
        assertEquals("ADM001", students.get(0).admissionNumber());
        assertEquals("Jane Doe", students.get(0).fullName());
    }

    @Test
    void resultSubjectsReturnsAssignedSubjects() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(
                assignments(classId, streamId, subjectId, academicYearId, "Grade 10", "Math"));
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName("Mathematics");
        subject.setCode("MATH101");
        subject.setSchoolId(schoolId);
        subject.setDeletedAt(null);
        when(subjectRepository.findAllById(anySet())).thenReturn(List.of(subject));

        List<TeacherResultDtos.SubjectOption> subjects = service.resultSubjects(classId, streamId, academicYearId);

        assertEquals(1, subjects.size());
        assertEquals(subjectId, subjects.get(0).id());
        assertEquals("Mathematics", subjects.get(0).name());
        assertEquals("MATH101", subjects.get(0).code());
    }

    @Test
    void resultExaminationsReturnsOpenExaminations() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(
                assignments(classId, streamId, subjectId, academicYearId, "Grade 10", "Math"));
        when(assignmentRepository
                .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
                        teacherId, schoolId, subjectId, classId, streamId, academicYearId)).thenReturn(true);
        when(academicYearRepository.findById(academicYearId))
                .thenReturn(Optional.of(new com.edumanage.academicyears.entity.AcademicYear()));
        ExaminationSubject es = new ExaminationSubject();
        es.setId(100L);
        es.setExaminationId(200L);
        es.setSubjectId(subjectId);
        es.setClassId(classId);
        es.setStreamId(streamId);
        es.setMaximumMarks(new BigDecimal("100"));
        es.setPassMark(new BigDecimal("40"));
        es.setGradingSchemeId(60L);
        es.setDeletedAt(null);
        Examination examination = new Examination();
        examination.setId(200L);
        examination.setSchoolId(schoolId);
        examination.setAcademicYearId(academicYearId);
        examination.setName("Mid Term");
        examination.setStatus("ACTIVE");
        examination.setStartDate(LocalDate.of(2024, 6, 1));
        examination.setEndDate(LocalDate.of(2024, 6, 10));
        ExaminationType type = new ExaminationType();
        type.setName("Mid Term");
        examination.setType(type);
        examination.setDeletedAt(null);
        es.setExamination(examination);
        when(examinationSubjectRepository.findBySchoolIdAndClassIdAndSubjectIdAndAcademicYearId(
                schoolId, classId, subjectId, academicYearId)).thenReturn(List.of(es));

        List<TeacherResultDtos.ExaminationOption> exams = service.resultExaminations(classId, streamId, subjectId, academicYearId);

        assertEquals(1, exams.size());
        assertEquals(200L, exams.get(0).id());
        assertEquals(100L, exams.get(0).examinationSubjectId());
        assertEquals("Mid Term", exams.get(0).examinationName());
        assertEquals(new BigDecimal("100"), exams.get(0).maximumMarks());
        assertEquals(60L, exams.get(0).gradingSchemeId());
    }

    @Test
    void resultExaminationsExcludesDraftExaminations() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(
                assignments(classId, streamId, subjectId, academicYearId, "Grade 10", "Math"));
        when(assignmentRepository
                .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
                        teacherId, schoolId, subjectId, classId, streamId, academicYearId)).thenReturn(true);
        when(academicYearRepository.findById(academicYearId))
                .thenReturn(Optional.of(new com.edumanage.academicyears.entity.AcademicYear()));
        ExaminationSubject es = new ExaminationSubject();
        es.setId(100L);
        es.setExaminationId(200L);
        es.setSubjectId(subjectId);
        es.setClassId(classId);
        es.setStreamId(streamId);
        es.setMaximumMarks(new BigDecimal("100"));
        es.setDeletedAt(null);
        Examination examination = new Examination();
        examination.setId(200L);
        examination.setSchoolId(schoolId);
        examination.setStatus("DRAFT");
        examination.setDeletedAt(null);
        es.setExamination(examination);
        when(examinationSubjectRepository.findBySchoolIdAndClassIdAndSubjectIdAndAcademicYearId(
                schoolId, classId, subjectId, academicYearId)).thenReturn(List.of(es));

        List<TeacherResultDtos.ExaminationOption> exams = service.resultExaminations(classId, streamId, subjectId, academicYearId);
        assertTrue(exams.isEmpty());
    }

    @Test
    void resultExaminationsRejectsUnassignedSubject() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(List.of());
        assertThrows(ForbiddenException.class,
                () -> service.resultExaminations(classId, streamId, subjectId, academicYearId));
    }

    @Test
    void listTeacherResultsReturnsTeacherResults() {
        when(assignmentService.listByTeacher(teacherId)).thenReturn(List.of());
        Result result = new Result();
        result.setId(1L);
        result.setSchoolId(schoolId);
        result.setStudentId(10L);
        result.setExaminationId(200L);
        result.setTeacherId(teacherId);
        result.setMarksObtained(new BigDecimal("85"));
        result.setGrade("A");
        result.setStatus("DRAFT");
        result.setCreatedAt(LocalDateTime.now());
        result.setDeletedAt(null);
        ExaminationSubject es = new ExaminationSubject();
        es.setId(100L);
        es.setSubjectId(subjectId);
        es.setClassId(classId);
        es.setStreamId(streamId);
        es.setMaximumMarks(new BigDecimal("100"));
        es.setDeletedAt(null);
        result.setExaminationSubject(es);
        StudentProfile student = new StudentProfile();
        student.setId(10L);
        student.setSchoolId(schoolId);
        student.setFirstName("Jane");
        student.setLastName("Doe");
        student.setAdmissionNumber("ADM001");
        student.setDeletedAt(null);
        result.setStudent(student);
        Examination examination = new Examination();
        examination.setId(200L);
        examination.setSchoolId(schoolId);
        examination.setName("Mid Term");
        examination.setDeletedAt(null);
        result.setExamination(examination);
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName("Mathematics");
        subject.setDeletedAt(null);
        es.setSubject(subject);
        com.edumanage.classes.entity.Class clazz = new com.edumanage.classes.entity.Class();
        clazz.setId(classId);
        clazz.setName("Grade 10");
        clazz.setDeletedAt(null);
        es.setClazz(clazz);
        com.edumanage.streams.entity.Stream stream = new com.edumanage.streams.entity.Stream();
        stream.setId(streamId);
        stream.setName("East");
        stream.setDeletedAt(null);
        es.setStream(stream);
        when(resultRepository.findByTeacherIdAndSchoolIdAndDeletedAtIsNull(teacherId, schoolId))
                .thenReturn(List.of(result));

        List<TeacherResultDtos.ResultRecord> records = service.listTeacherResults(null, null, null, null, null);

        assertEquals(1, records.size());
        assertEquals(1L, records.get(0).id());
        assertEquals("Jane Doe", records.get(0).studentName());
        assertEquals("ADM001", records.get(0).admissionNumber());
        assertEquals("A", records.get(0).grade());
        assertEquals("DRAFT", records.get(0).status());
    }
}
