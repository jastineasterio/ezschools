package com.edumanage.results.service;

import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.BadRequestException;
import com.edumanage.results.dto.CreateResultRequest;
import com.edumanage.results.dto.ResultDto;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.examinationsubjects.entity.ExaminationSubject;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.roles.entity.Role;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.security.AccessGuard;
import com.edumanage.security.CurrentUserService;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.gradingschemes.entity.GradingScheme;
import com.edumanage.gradingschemes.repository.GradingSchemeRepository;
import com.edumanage.gradingschemeranges.entity.GradingSchemeRange;
import com.edumanage.gradingschemeranges.repository.GradingSchemeRangeRepository;
import com.edumanage.results.entity.GradeSnapshot;
import com.edumanage.results.repository.GradeSnapshotRepository;
import com.edumanage.resultapprovals.repository.ResultApprovalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultServiceTest {

    @Mock private ResultRepository resultRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private ExaminationSubjectRepository examinationSubjectRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private AccessGuard accessGuard;
    @Mock private CurrentUserService currentUserService;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private GradingSchemeRepository gradingSchemeRepository;
    @Mock private GradingSchemeRangeRepository gradingSchemeRangeRepository;
    @Mock private GradeSnapshotRepository gradeSnapshotRepository;
    @Mock private ResultApprovalRepository resultApprovalRepository;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private ResultService service;

    private final Long schoolId = 7L;
    private final Long teacherId = 1L;
    private final Long studentId = 10L;
    private final Long examSubjectId = 100L;
    private final Long subjectId = 50L;
    private final Long classId = 30L;
    private final Long streamId = 40L;
    private final Long academicYearId = 20L;
    private final Long schemeId = 60L;
    private final Long examinationId = 200L;

    private User teacher;
    private User actor;
    private StudentProfile student;
    private ExaminationSubject examSubject;
    private Examination examination;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(schoolId);

        Role teacherRole = new Role();
        teacherRole.setName("TEACHER");

        teacher = new User();
        teacher.setId(teacherId);
        teacher.setEmail("teacher@school.test");
        teacher.setSchoolId(schoolId);
        teacher.setRoles(Set.of(teacherRole));

        actor = new User();
        actor.setId(teacherId);
        actor.setEmail("teacher@school.test");
        actor.setSchoolId(schoolId);
        actor.setRoles(Set.of(teacherRole));

        student = new StudentProfile();
        student.setId(studentId);
        student.setSchoolId(schoolId);
        student.setFirstName("Jane");
        student.setLastName("Doe");
        student.setAdmissionNumber("ADM001");
        student.setDeletedAt(null);

        examSubject = new ExaminationSubject();
        examSubject.setId(examSubjectId);
        examSubject.setSubjectId(subjectId);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        examSubject.setExaminationId(examinationId);
        examSubject.setMaximumMarks(new BigDecimal("100"));
        examSubject.setPassMark(new BigDecimal("40"));
        examSubject.setDeletedAt(null);

        examination = new Examination();
        examination.setId(examinationId);
        examination.setSchoolId(schoolId);
        examination.setAcademicYearId(academicYearId);
        examination.setDeletedAt(null);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void mockCommonPreconditions() {
        when(currentUserService.requireCurrentUser()).thenReturn(actor);
        when(currentUserService.hasRole(actor, "TEACHER")).thenReturn(true);
        when(currentUserService.hasRole(actor, "MANAGER")).thenReturn(false);
        when(currentUserService.hasAnyRole(actor, "TEACHER", "MANAGER")).thenReturn(true);
        when(studentProfileRepository.findById(studentId))
                .thenReturn(Optional.of(student));
        when(examinationSubjectRepository.findById(examSubjectId))
                .thenReturn(Optional.of(examSubject));
        when(examinationRepository.findById(examinationId))
                .thenReturn(Optional.of(examination));
        when(teacherAssignmentRepository
                .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
                        teacherId, schoolId, subjectId, classId, streamId, academicYearId))
                .thenReturn(true);
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setSchoolId(schoolId);
        enrollment.setClassId(classId);
        enrollment.setStreamId(streamId);
        enrollment.setAcademicYearId(academicYearId);
        enrollment.setStatus("ENROLLED");
        enrollment.setDeletedAt(null);
        enrollment.setStudent(student);
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId))
                .thenReturn(Optional.of(enrollment));
        when(resultRepository.findByStudentIdAndExaminationSubjectId(studentId, examSubjectId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(resultRepository.save(any(Result.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createCalculatesGradeFromGradingScheme() {
        examSubject.setGradingSchemeId(schemeId);
        GradingScheme scheme = new GradingScheme();
        scheme.setId(schemeId);
        scheme.setSchoolId(schoolId);
        scheme.setStatus("ACTIVE");
        scheme.setDeletedAt(null);
        GradingSchemeRange rangeA = new GradingSchemeRange();
        rangeA.setGrade("A");
        rangeA.setMinMark(new BigDecimal("80"));
        rangeA.setMaxMark(new BigDecimal("100"));
        rangeA.setGradePoint(new BigDecimal("4.0"));
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(rangeA));

        mockCommonPreconditions();

        CreateResultRequest request = new CreateResultRequest();
        request.setStudentId(studentId);
        request.setExaminationSubjectId(examSubjectId);
        request.setMarksObtained(85);
        request.setRemarks(null);

        ResultDto result = service.create(request);

        assertEquals("A", result.getGrade());
        assertEquals(4.0, result.getGradePoint());
    }

    @Test
    void createRecalculatesGradeWhenMarksChange() {
        examSubject.setGradingSchemeId(schemeId);
        GradingScheme scheme = new GradingScheme();
        scheme.setId(schemeId);
        scheme.setSchoolId(schoolId);
        scheme.setStatus("ACTIVE");
        scheme.setDeletedAt(null);
        GradingSchemeRange rangeA = new GradingSchemeRange();
        rangeA.setGrade("A");
        rangeA.setMinMark(new BigDecimal("80"));
        rangeA.setMaxMark(new BigDecimal("100"));
        GradingSchemeRange rangeC = new GradingSchemeRange();
        rangeC.setGrade("C");
        rangeC.setMinMark(new BigDecimal("60"));
        rangeC.setMaxMark(new BigDecimal("69"));
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(rangeA, rangeC));

        mockCommonPreconditions();

        CreateResultRequest requestA = new CreateResultRequest();
        requestA.setStudentId(studentId);
        requestA.setExaminationSubjectId(examSubjectId);
        requestA.setMarksObtained(85);
        ResultDto resultA = service.create(requestA);
        assertEquals("A", resultA.getGrade());

        CreateResultRequest requestC = new CreateResultRequest();
        requestC.setStudentId(studentId);
        requestC.setExaminationSubjectId(examSubjectId);
        requestC.setMarksObtained(65);
        ResultDto resultC = service.create(requestC);
        assertEquals("C", resultC.getGrade());
    }

    @Test
    void createRejectsWhenNoGradingSchemeConfigured() {
        examSubject.setGradingSchemeId(null);
        mockCommonPreconditions();

        CreateResultRequest request = new CreateResultRequest();
        request.setStudentId(studentId);
        request.setExaminationSubjectId(examSubjectId);
        request.setMarksObtained(85);

        assertThrows(BadRequestException.class, () -> service.create(request));
    }

    @Test
    void createRejectsWhenMarksOutsideAllRanges() {
        examSubject.setGradingSchemeId(schemeId);
        GradingScheme scheme = new GradingScheme();
        scheme.setId(schemeId);
        scheme.setSchoolId(schoolId);
        scheme.setStatus("ACTIVE");
        scheme.setDeletedAt(null);
        GradingSchemeRange rangeA = new GradingSchemeRange();
        rangeA.setGrade("A");
        rangeA.setMinMark(new BigDecimal("80"));
        rangeA.setMaxMark(new BigDecimal("100"));
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(rangeA));

        mockCommonPreconditions();

        CreateResultRequest request = new CreateResultRequest();
        request.setStudentId(studentId);
        request.setExaminationSubjectId(examSubjectId);
        request.setMarksObtained(45);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("do not fall within any configured grading range"));
    }

    @Test
    void createRejectsOverlappingGradingRanges() {
        examSubject.setGradingSchemeId(schemeId);
        GradingScheme scheme = new GradingScheme();
        scheme.setId(schemeId);
        scheme.setSchoolId(schoolId);
        scheme.setStatus("ACTIVE");
        scheme.setDeletedAt(null);
        GradingSchemeRange rangeA = new GradingSchemeRange();
        rangeA.setGrade("A");
        rangeA.setMinMark(new BigDecimal("80"));
        rangeA.setMaxMark(new BigDecimal("100"));
        GradingSchemeRange rangeB = new GradingSchemeRange();
        rangeB.setGrade("B");
        rangeB.setMinMark(new BigDecimal("90"));
        rangeB.setMaxMark(new BigDecimal("100"));
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(rangeA, rangeB));

        mockCommonPreconditions();

        CreateResultRequest request = new CreateResultRequest();
        request.setStudentId(studentId);
        request.setExaminationSubjectId(examSubjectId);
        request.setMarksObtained(95);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("Overlapping grading ranges"));
    }

    @Test
    void createDoesNotAssignGradeWhenMarksIsNull() {
        examSubject.setGradingSchemeId(schemeId);
        GradingScheme scheme = new GradingScheme();
        scheme.setId(schemeId);
        scheme.setSchoolId(schoolId);
        scheme.setStatus("ACTIVE");
        scheme.setDeletedAt(null);
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of());

        mockCommonPreconditions();

        CreateResultRequest request = new CreateResultRequest();
        request.setStudentId(studentId);
        request.setExaminationSubjectId(examSubjectId);
        request.setMarksObtained(null);

        ResultDto result = service.create(request);

        assertNull(result.getGrade());
        assertNull(result.getGradePoint());
    }

    @Test
    void submitPersistsGradeSnapshot() {
        examSubject.setGradingSchemeId(schemeId);
        GradingScheme scheme = new GradingScheme();
        scheme.setId(schemeId);
        scheme.setSchoolId(schoolId);
        scheme.setStatus("ACTIVE");
        scheme.setDeletedAt(null);
        GradingSchemeRange rangeA = new GradingSchemeRange();
        rangeA.setGrade("A");
        rangeA.setMinMark(new BigDecimal("80"));
        rangeA.setMaxMark(new BigDecimal("100"));
        rangeA.setGradePoint(new BigDecimal("4.0"));
        when(gradingSchemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId))
                .thenReturn(List.of(rangeA));
        when(objectMapper.valueToTree(any(List.class))).thenReturn(JsonNodeFactory.instance.arrayNode());

        mockCommonPreconditions();

        Result result = new Result();
        result.setId(1L);
        result.setSchoolId(schoolId);
        result.setStudentId(studentId);
        result.setExaminationSubjectId(examSubjectId);
        result.setExaminationId(examinationId);
        result.setTeacherId(teacherId);
        result.setGradingSchemeId(schemeId);
        result.setGradingSchemeName("Scheme");
        result.setMarksObtained(new BigDecimal("85"));
        result.setPercentage(new BigDecimal("85"));
        result.setGrade("A");
        result.setStatus("DRAFT");
        result.setDeletedAt(null);
        when(resultRepository.findById(1L)).thenReturn(Optional.of(result));
        when(resultRepository.save(any(Result.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResultDto submitted = service.submit(1L);

        assertEquals("SUBMITTED", submitted.getStatus());
        assertNotNull(submitted.getSubmittedAt());

        ArgumentCaptor<GradeSnapshot> captor = ArgumentCaptor.forClass(GradeSnapshot.class);
        verify(gradeSnapshotRepository).save(captor.capture());
        GradeSnapshot snapshot = captor.getValue();
        assertEquals(1L, snapshot.getResultId());
        assertEquals(schoolId, snapshot.getSchoolId());
        assertEquals(schemeId, snapshot.getGradingSchemeId());
        assertNotNull(snapshot.getGradeRanges());
        assertTrue(snapshot.getGradeRanges() instanceof JsonNode,
                "grade_ranges must be persisted as structured JSON, not a String");
        JsonNode ranges = snapshot.getGradeRanges();
        assertTrue(ranges.isArray());
    }

    @Test
    void submitRejectsNonDraftResult() {
        mockCommonPreconditions();
        Result result = new Result();
        result.setId(1L);
        result.setSchoolId(schoolId);
        result.setTeacherId(teacherId);
        result.setStatus("SUBMITTED");
        result.setDeletedAt(null);
        when(resultRepository.findById(1L)).thenReturn(Optional.of(result));

        assertThrows(BadRequestException.class, () -> service.submit(1L));
    }

    @Test
    void listReturnsStudentAndExaminationDetails() {
        com.edumanage.classes.entity.Class clazz = new com.edumanage.classes.entity.Class();
        clazz.setId(classId);
        clazz.setName("Grade 10");
        clazz.setDeletedAt(null);
        com.edumanage.streams.entity.Stream stream = new com.edumanage.streams.entity.Stream();
        stream.setId(streamId);
        stream.setName("East");
        stream.setDeletedAt(null);
        com.edumanage.subjects.entity.Subject subject = new com.edumanage.subjects.entity.Subject();
        subject.setId(subjectId);
        subject.setName("Mathematics");
        subject.setDeletedAt(null);
        Examination examination = new Examination();
        examination.setId(examinationId);
        examination.setName("Mid Term");
        examination.setDeletedAt(null);
        ExaminationSubject es = new ExaminationSubject();
        es.setId(examSubjectId);
        es.setSubjectId(subjectId);
        es.setClassId(classId);
        es.setStreamId(streamId);
        es.setExaminationId(examinationId);
        es.setExamination(examination);
        es.setSubject(subject);
        es.setClazz(clazz);
        es.setStream(stream);
        es.setDeletedAt(null);
        User teacher = new User();
        teacher.setId(teacherId);
        teacher.setFirstName("John");
        teacher.setLastName("Smith");
        when(currentUserService.displayName(teacher)).thenReturn("John Smith");

        Result result = new Result();
        result.setId(1L);
        result.setSchoolId(schoolId);
        result.setStudentId(studentId);
        result.setExaminationSubjectId(examSubjectId);
        result.setExaminationId(examinationId);
        result.setTeacherId(teacherId);
        result.setStudent(student);
        result.setExaminationSubject(es);
        result.setTeacher(teacher);
        result.setDeletedAt(null);

        Page<Result> page = new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1);
        when(resultRepository.findByFilters(schoolId, null, null, null, null, PageRequest.of(0, 10))).thenReturn(page);

        PageResponse<ResultDto> response = service.list(PageRequest.of(0, 10));
        assertEquals(1, response.getContent().size());
        ResultDto dto = response.getContent().get(0);
        assertEquals("Jane Doe", dto.getStudentName());
        assertEquals("ADM001", dto.getAdmissionNumber());
        assertEquals("Mid Term", dto.getExaminationName());
        assertEquals("Mathematics", dto.getSubjectName());
        assertEquals("Grade 10", dto.getClassName());
        assertEquals("East", dto.getStreamName());
        assertEquals(streamId, dto.getStreamId());
        assertEquals(classId, dto.getClassId());
        assertEquals(subjectId, dto.getSubjectId());
        assertEquals("John Smith", dto.getTeacherName());
    }

    private static Role teacherRole() {
        Role role = new Role();
        role.setName("TEACHER");
        return role;
    }
}
