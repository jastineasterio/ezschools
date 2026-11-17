package com.edumanage.resultsnotifications.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.common.util.AuditAware;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.examinations.dto.ExaminationDto;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.examinationsubjects.entity.ExaminationSubject;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.notifications.service.DarsmsProviderService;
import com.edumanage.notifications.service.NotificationProviderException;
import com.edumanage.parents.entity.Parent;
import com.edumanage.parents.repository.ParentRepository;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.resultsnotifications.dto.ResultNotificationDeliveryDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationTemplateDto;
import com.edumanage.resultsnotifications.service.ResultNotificationTemplateService;
import com.edumanage.resultsnotifications.dto.ResultNotificationPreview;
import com.edumanage.resultsnotifications.dto.ResultNotificationRequest;
import com.edumanage.resultsnotifications.entity.ResultNotification;
import com.edumanage.resultsnotifications.entity.ResultNotificationDelivery;
import com.edumanage.resultsnotifications.repository.ResultNotificationDeliveryRepository;
import com.edumanage.resultsnotifications.repository.ResultNotificationRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.sms.dto.CreateSmsMessageRequest;
import com.edumanage.sms.dto.SmsMessageDto;
import com.edumanage.sms.entity.SmsMessage;
import com.edumanage.sms.repository.SmsMessageRepository;
import com.edumanage.sms.service.SmsMessageService;
import com.edumanage.streams.dto.StreamDto;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.studentparents.entity.StudentParent;
import com.edumanage.studentparents.repository.StudentParentRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.tenant.TenantContext;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultNotificationServiceTest {

    @Mock private ResultNotificationRepository notificationRepository;
    @Mock private ResultNotificationDeliveryRepository deliveryRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private ClassRepository classRepository;
    @Mock private StreamRepository streamRepository;
    @Mock private ExaminationSubjectRepository examinationSubjectRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private StudentParentRepository studentParentRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private SmsMessageService smsMessageService;
    @Mock private SmsMessageRepository smsMessageRepository;
    @Mock private ResultNotificationTemplateService resultNotificationTemplateService;
    @Mock private DarsmsProviderService darsmsProviderService;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ResultNotificationService service;

    private final Long schoolId = 1L;
    private final Long examinationId = 10L;
    private final Long classId = 20L;
    private final Long streamId = 30L;
    private final Long studentId = 100L;
    private final Long parentId = 200L;

    private Examination examination;
    private com.edumanage.classes.entity.Class schoolClass;
    private Stream stream;
    private StudentProfile student;
    private Parent parent;
    private StudentParent studentParent;
    private Result result;
    private School school;

    @BeforeEach
    void setUp() {
        examination = new Examination();
        examination.setId(examinationId);
        examination.setSchoolId(schoolId);
        examination.setName("Mid-Term Exam");

        schoolClass = new Class();
        schoolClass.setId(classId);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setName("Form 1");

        stream = new Stream();
        stream.setId(streamId);
        stream.setSchoolId(schoolId);
        stream.setClassId(classId);
        stream.setName("A");

        student = new StudentProfile();
        student.setId(studentId);
        student.setSchoolId(schoolId);
        student.setAdmissionNumber("ADM001");
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setPhone("0712345678");

        parent = new Parent();
        parent.setId(parentId);
        parent.setSchoolId(schoolId);
        parent.setFirstName("Jane");
        parent.setLastName("Doe");
        parent.setPhone("0787654321");

        studentParent = new StudentParent();
        studentParent.setStudentId(studentId);
        studentParent.setParentId(parentId);
        studentParent.setRelationshipType("FATHER");
        studentParent.setPrimary(true);
        studentParent.setReceivesSms(true);

        ExaminationSubject examSubject = new ExaminationSubject();
        examSubject.setId(2000L);
        examSubject.setExaminationId(examinationId);
        examSubject.setSubjectId(3000L);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        examSubject.setMaximumMarks(new BigDecimal("100"));
        examSubject.setPassMark(new BigDecimal("40"));

        result = new Result();
        result.setId(1000L);
        result.setSchoolId(schoolId);
        result.setStudentId(studentId);
        result.setExaminationId(examinationId);
        result.setExaminationSubjectId(2000L);
        result.setStatus("PUBLISHED");
        result.setMarksObtained(new BigDecimal("78"));
        result.setGrade("B");
        result.setExaminationSubject(examSubject);

        school = new School();
        school.setId(schoolId);
        school.setName("MJ School");

        lenient().when(resultNotificationTemplateService.getTemplate(schoolId))
                .thenReturn(new ResultNotificationTemplateDto(schoolId,
                        "Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}",
                        true, null, List.of()));

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void authAsManager(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("manager@school.com");
        user.setSchoolId(schoolId);
        user.setActive(true);
        var auth = new UsernamePasswordAuthenticationToken(
                new com.edumanage.security.CustomUserDetails(userId, "manager@school.com", "pass", schoolId, true, java.util.List.of()),
                null, java.util.List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(schoolId);
        when(userRepository.findByEmail("manager@school.com")).thenReturn(Optional.of(user));
    }

    @Test
    void validateScopeRejectsExaminationFromAnotherSchool() {
        when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.validateScope(schoolId, examinationId, classId, streamId));
    }

    @Test
    void validateScopeRejectsClassFromAnotherSchool() {
        when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.validateScope(schoolId, examinationId, classId, streamId));
    }

    @Test
    void validateScopeRejectsStreamFromAnotherSchool() {
        when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.validateScope(schoolId, examinationId, classId, streamId));
    }

    @Test
    void validateScopeRejectsStreamNotBelongingToClass() {
        Stream otherStream = new Stream();
        otherStream.setId(99L);
        otherStream.setSchoolId(schoolId);
        otherStream.setClassId(999L);

        when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(otherStream));
        when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        assertThrows(BadRequestException.class, () ->
                service.validateScope(schoolId, examinationId, classId, streamId));
    }

    @Test
    void getExaminationsReturnsOnlySchoolExaminations() {
        lenient().when(examinationRepository.findBySchoolId(schoolId, PageRequest.of(0, 500)))
                .thenReturn(new PageImpl<>(List.of(examination)));

        List<ExaminationDto> exams = service.getExaminations(schoolId);
        assertEquals(1, exams.size());
        assertEquals(examinationId, exams.get(0).getId());
    }

    @Test
    void getClassesReturnsOnlySchoolClasses() {
        lenient().when(classRepository.findBySchoolId(schoolId, PageRequest.of(0, 500)))
                .thenReturn(new PageImpl<>(List.of(schoolClass)));

        List<Class> classes = service.getClasses(schoolId);
        assertEquals(1, classes.size());
        assertEquals(classId, classes.get(0).getId());
    }

    @Test
    void getStreamsReturnsOnlyStreamsForClassAndSchool() {
        lenient().when(streamRepository.findByClassIdAndSchoolId(classId, schoolId))
                .thenReturn(List.of(stream));

        List<StreamDto> streams = service.getStreams(schoolId, classId);
        assertEquals(1, streams.size());
        assertEquals(streamId, streams.get(0).getId());
    }

    @Test
    void previewExcludesDraftResults() {
        ExaminationSubject draftExamSubject = new ExaminationSubject();
        draftExamSubject.setId(2001L);
        draftExamSubject.setExaminationId(examinationId);
        draftExamSubject.setClassId(classId);
        draftExamSubject.setStreamId(streamId);
        draftExamSubject.setMaximumMarks(new BigDecimal("100"));

        Result draftResult = new Result();
        draftResult.setId(1001L);
        draftResult.setSchoolId(schoolId);
        draftResult.setStudentId(studentId);
        draftResult.setExaminationId(examinationId);
        draftResult.setExaminationSubjectId(2001L);
        draftResult.setStatus("DRAFT");
        draftResult.setExaminationSubject(draftExamSubject);

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(draftExamSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                eq(schoolId), eq(examinationId), eq(classId), eq(streamId), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of());
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        ResultNotificationPreview preview = service.preview(schoolId, examinationId, classId, streamId, "STUDENTS");

        assertEquals(0, preview.eligibleCount());
    }

    @Test
    void previewIncludesPublishedResults() {
        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));

        ExaminationSubject examSubject = new ExaminationSubject();
        examSubject.setId(2000L);
        examSubject.setExaminationId(examinationId);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(examSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                eq(schoolId), eq(examinationId), eq(classId), eq(streamId), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(result));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId)))
                .thenReturn(List.of(student));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        ResultNotificationPreview preview = service.preview(schoolId, examinationId, classId, streamId, "STUDENTS");

        assertEquals(1, preview.eligibleCount());
        assertEquals(0, preview.excludedCount());
        assertTrue(preview.canSend());
    }

    @Test
    void previewExcludesStudentWithMissingPhone() {
        StudentProfile noPhoneStudent = new StudentProfile();
        noPhoneStudent.setId(101L);
        noPhoneStudent.setSchoolId(schoolId);
        noPhoneStudent.setAdmissionNumber("ADM002");
        noPhoneStudent.setFirstName("No");
        noPhoneStudent.setLastName("Phone");
        noPhoneStudent.setPhone(null);

        ExaminationSubject noPhoneExamSubject = new ExaminationSubject();
        noPhoneExamSubject.setId(2001L);
        noPhoneExamSubject.setExaminationId(examinationId);
        noPhoneExamSubject.setClassId(classId);
        noPhoneExamSubject.setStreamId(streamId);
        noPhoneExamSubject.setMaximumMarks(new BigDecimal("100"));

        Result noPhoneResult = new Result();
        noPhoneResult.setId(1001L);
        noPhoneResult.setSchoolId(schoolId);
        noPhoneResult.setStudentId(101L);
        noPhoneResult.setExaminationId(examinationId);
        noPhoneResult.setExaminationSubjectId(2001L);
        noPhoneResult.setStatus("PUBLISHED");
        noPhoneResult.setMarksObtained(new BigDecimal("50"));
        noPhoneResult.setExaminationSubject(noPhoneExamSubject);

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(noPhoneExamSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(noPhoneResult));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(101L)))
                .thenReturn(List.of(noPhoneStudent));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        ResultNotificationPreview preview = service.preview(schoolId, examinationId, classId, streamId, "STUDENTS");

        assertEquals(0, preview.eligibleCount());
        assertEquals(1, preview.excludedCount());
        assertFalse(preview.canSend());
        assertNotNull(preview.reason());
    }

    @Test
    void previewExcludesStudentWithInvalidPhone() {
        StudentProfile invalidPhoneStudent = new StudentProfile();
        invalidPhoneStudent.setId(101L);
        invalidPhoneStudent.setSchoolId(schoolId);
        invalidPhoneStudent.setAdmissionNumber("ADM002");
        invalidPhoneStudent.setFirstName("Bad");
        invalidPhoneStudent.setLastName("Phone");
        invalidPhoneStudent.setPhone("invalid");

        ExaminationSubject invalidExamSubject = new ExaminationSubject();
        invalidExamSubject.setId(2001L);
        invalidExamSubject.setExaminationId(examinationId);
        invalidExamSubject.setClassId(classId);
        invalidExamSubject.setStreamId(streamId);
        invalidExamSubject.setMaximumMarks(new BigDecimal("100"));

        Result invalidResult = new Result();
        invalidResult.setId(1001L);
        invalidResult.setSchoolId(schoolId);
        invalidResult.setStudentId(101L);
        invalidResult.setExaminationId(examinationId);
        invalidResult.setExaminationSubjectId(2001L);
        invalidResult.setStatus("PUBLISHED");
        invalidResult.setMarksObtained(new BigDecimal("50"));
        invalidResult.setExaminationSubject(invalidExamSubject);

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(invalidExamSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(invalidResult));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(101L)))
                .thenReturn(List.of(invalidPhoneStudent));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        ResultNotificationPreview preview = service.preview(schoolId, examinationId, classId, streamId, "STUDENTS");

        assertEquals(0, preview.eligibleCount());
        assertEquals(1, preview.excludedCount());
    }

    @Test
    void previewParentRecipientResolvesPrimaryGuardian() {
        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        ExaminationSubject examSubject = new ExaminationSubject();
        examSubject.setId(2000L);
        examSubject.setExaminationId(examinationId);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(examSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(result));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId)))
                .thenReturn(List.of(student));
        lenient().when(studentParentRepository.findByStudentId(studentId))
                .thenReturn(List.of(studentParent));
        lenient().when(parentRepository.findBySchoolIdAndId(schoolId, parentId))
                .thenReturn(Optional.of(parent));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        ResultNotificationPreview preview = service.preview(schoolId, examinationId, classId, streamId, "PARENTS");

        assertEquals(1, preview.eligibleCount());
        assertEquals(0, preview.excludedCount());
        assertEquals(1, preview.parents().size());
        assertTrue(preview.parents().get(0).eligible());
    }

    @Test
    void previewParentWithMissingPhoneIsExcluded() {
        Parent noPhoneParent = new Parent();
        noPhoneParent.setId(parentId);
        noPhoneParent.setSchoolId(schoolId);
        noPhoneParent.setFirstName("No");
        noPhoneParent.setLastName("Phone");
        noPhoneParent.setPhone(null);

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        ExaminationSubject examSubject = new ExaminationSubject();
        examSubject.setId(2000L);
        examSubject.setExaminationId(examinationId);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(examSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(result));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId)))
                .thenReturn(List.of(student));
        lenient().when(studentParentRepository.findByStudentId(studentId))
                .thenReturn(List.of(studentParent));
        lenient().when(parentRepository.findBySchoolIdAndId(schoolId, parentId))
                .thenReturn(Optional.of(noPhoneParent));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        ResultNotificationPreview preview = service.preview(schoolId, examinationId, classId, streamId, "PARENTS");

        assertEquals(0, preview.eligibleCount());
        assertEquals(1, preview.excludedCount());
        assertFalse(preview.parents().get(0).eligible());
    }

    @Test
    void previewBothRecipientsIncludesStudentsAndParents() {
        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        ExaminationSubject examSubject = new ExaminationSubject();
        examSubject.setId(2000L);
        examSubject.setExaminationId(examinationId);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(examSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(result));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId)))
                .thenReturn(List.of(student));
        lenient().when(studentParentRepository.findByStudentId(studentId))
                .thenReturn(List.of(studentParent));
        lenient().when(parentRepository.findBySchoolIdAndId(schoolId, parentId))
                .thenReturn(Optional.of(parent));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        ResultNotificationPreview preview = service.preview(schoolId, examinationId, classId, streamId, "BOTH");

        assertEquals(2, preview.eligibleCount());
        assertEquals(0, preview.excludedCount());
        assertEquals(1, preview.students().size());
        assertEquals(1, preview.parents().size());
        assertTrue(preview.canSend());
    }

    @Test
    void previewPreventsDuplicateParentPhone() {
        Parent duplicateParent = new Parent();
        duplicateParent.setId(300L);
        duplicateParent.setSchoolId(schoolId);
        duplicateParent.setFirstName("Dup");
        duplicateParent.setLastName("Parent");
        duplicateParent.setPhone("0787654321");

        StudentParent duplicateRelationship = new StudentParent();
        duplicateRelationship.setStudentId(studentId);
        duplicateRelationship.setParentId(300L);
        duplicateRelationship.setRelationshipType("MOTHER");
        duplicateRelationship.setPrimary(false);
        duplicateRelationship.setReceivesSms(true);

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        ExaminationSubject examSubject = new ExaminationSubject();
        examSubject.setId(2000L);
        examSubject.setExaminationId(examinationId);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(examSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(result));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId)))
                .thenReturn(List.of(student));
        lenient().when(studentParentRepository.findByStudentId(studentId))
                .thenReturn(List.of(studentParent, duplicateRelationship));
        lenient().when(parentRepository.findBySchoolIdAndId(schoolId, parentId))
                .thenReturn(Optional.of(parent));
        lenient().when(parentRepository.findBySchoolIdAndId(schoolId, 300L))
                .thenReturn(Optional.of(duplicateParent));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        ResultNotificationPreview preview = service.preview(schoolId, examinationId, classId, streamId, "PARENTS");

        assertEquals(1, preview.eligibleCount());
        assertEquals(1, preview.excludedCount());
    }

    @Test
    void sendIsIdempotent() {
        String idempotencyKey = UUID.randomUUID().toString();
        ResultNotification existing = new ResultNotification();
        existing.setId(500L);
        existing.setIdempotencyKey(idempotencyKey);
        existing.setStatus("SENT");

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        ExaminationSubject esForIdempotent = new ExaminationSubject();
        esForIdempotent.setId(2000L);
        esForIdempotent.setExaminationId(examinationId);
        esForIdempotent.setClassId(classId);
        esForIdempotent.setStreamId(streamId);
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(esForIdempotent)));
        lenient().when(notificationRepository.findBySchoolIdAndIdempotencyKey(schoolId, idempotencyKey))
                .thenReturn(Optional.of(existing));

        ResultNotificationDto dto = service.send(
                schoolId, examinationId, classId, streamId, "STUDENTS", idempotencyKey, 10L);

        assertEquals(500L, dto.id());
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void sendCreatesNotificationAndDeliveries() {
        String idempotencyKey = UUID.randomUUID().toString();

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        ExaminationSubject examSubject = new ExaminationSubject();
        examSubject.setId(2000L);
        examSubject.setExaminationId(examinationId);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        examSubject.setMaximumMarks(new BigDecimal("100"));
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(examSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(result));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId)))
                .thenReturn(List.of(student));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        lenient().when(notificationRepository.findBySchoolIdAndIdempotencyKey(schoolId, idempotencyKey))
                .thenReturn(Optional.empty());

        SmsMessageDto sentDto = new SmsMessageDto();
        sentDto.setId(9000L);
        sentDto.setSchoolId(schoolId);
        sentDto.setRecipient("255712345678");
        sentDto.setMessage("Test message");
        sentDto.setStatus("PENDING");
        lenient().when(smsMessageService.sendSms(any(CreateSmsMessageRequest.class))).thenReturn(sentDto);

        ResultNotification saved = new ResultNotification();
        saved.setId(500L);
        saved.setIdempotencyKey(idempotencyKey);
        lenient().when(notificationRepository.saveAndFlush(any(ResultNotification.class))).thenReturn(saved);
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(saved));

        authAsManager(10L);

        ResultNotificationDto dto = service.send(
                schoolId, examinationId, classId, streamId, "STUDENTS", idempotencyKey, 10L);

        assertNotNull(dto);
        assertEquals(500L, dto.id());
        assertEquals(1, dto.totalRecipients());
        assertEquals(0, dto.excludedCount());
        verify(smsMessageService).sendSms(any(CreateSmsMessageRequest.class));
        verify(deliveryRepository).saveAllAndFlush(anyList());
        verify(auditLogService).log(anyString(), anyString(), anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void sendExcludesStudentWithMissingPhone() {
        String idempotencyKey = UUID.randomUUID().toString();
        StudentProfile noPhoneStudent = new StudentProfile();
        noPhoneStudent.setId(101L);
        noPhoneStudent.setSchoolId(schoolId);
        noPhoneStudent.setAdmissionNumber("ADM002");
        noPhoneStudent.setFirstName("No");
        noPhoneStudent.setLastName("Phone");
        noPhoneStudent.setPhone(null);

        ExaminationSubject noPhoneExamSubject = new ExaminationSubject();
        noPhoneExamSubject.setId(2001L);
        noPhoneExamSubject.setExaminationId(examinationId);
        noPhoneExamSubject.setClassId(classId);
        noPhoneExamSubject.setStreamId(streamId);
        noPhoneExamSubject.setMaximumMarks(new BigDecimal("100"));

        Result noPhoneResult = new Result();
        noPhoneResult.setId(1001L);
        noPhoneResult.setSchoolId(schoolId);
        noPhoneResult.setStudentId(101L);
        noPhoneResult.setExaminationId(examinationId);
        noPhoneResult.setExaminationSubjectId(2001L);
        noPhoneResult.setStatus("PUBLISHED");
        noPhoneResult.setMarksObtained(new BigDecimal("50"));
        noPhoneResult.setExaminationSubject(noPhoneExamSubject);

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(noPhoneExamSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(noPhoneResult));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(101L)))
                .thenReturn(List.of(noPhoneStudent));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        lenient().when(notificationRepository.findBySchoolIdAndIdempotencyKey(schoolId, idempotencyKey))
                .thenReturn(Optional.empty());

        ResultNotification saved = new ResultNotification();
        saved.setId(500L);
        saved.setIdempotencyKey(idempotencyKey);
        lenient().when(notificationRepository.saveAndFlush(any(ResultNotification.class))).thenReturn(saved);
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(saved));

        authAsManager(10L);

        ResultNotificationDto dto = service.send(
                schoolId, examinationId, classId, streamId, "STUDENTS", idempotencyKey, 10L);

        assertEquals(1, dto.totalRecipients());
        assertEquals(1, dto.excludedCount());
        assertEquals(0, dto.successfulCount());
        verify(smsMessageService, never()).sendSms(any());
    }

    @Test
    void sendSmsServiceFailureResultsInFailedDelivery() {
        String idempotencyKey = UUID.randomUUID().toString();

        lenient().when(examinationRepository.findBySchoolIdAndId(schoolId, examinationId))
                .thenReturn(Optional.of(examination));
        lenient().when(classRepository.findBySchoolIdAndId(schoolId, classId))
                .thenReturn(Optional.of(schoolClass));
        lenient().when(streamRepository.findBySchoolIdAndId(schoolId, streamId))
                .thenReturn(Optional.of(stream));
        ExaminationSubject examSubject = new ExaminationSubject();
        examSubject.setId(2000L);
        examSubject.setExaminationId(examinationId);
        examSubject.setClassId(classId);
        examSubject.setStreamId(streamId);
        examSubject.setMaximumMarks(new BigDecimal("100"));
        lenient().when(examinationSubjectRepository.findByExaminationId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(examSubject)));
        lenient().when(resultRepository.findByExaminationAndClassAndStreamAndStatus(
                any(), any(), any(), any(), eq(List.of("PUBLISHED", "APPROVED"))))
                .thenReturn(List.of(result));
        lenient().when(studentProfileRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId)))
                .thenReturn(List.of(student));
        lenient().when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        lenient().when(notificationRepository.findBySchoolIdAndIdempotencyKey(schoolId, idempotencyKey))
                .thenReturn(Optional.empty());
        lenient().when(smsMessageService.sendSms(any(CreateSmsMessageRequest.class)))
                .thenThrow(new RuntimeException("Provider down"));

        ResultNotification saved = new ResultNotification();
        saved.setId(500L);
        saved.setIdempotencyKey(idempotencyKey);
        lenient().when(notificationRepository.saveAndFlush(any(ResultNotification.class))).thenReturn(saved);
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(saved));

        authAsManager(10L);

        ResultNotificationDto dto = service.send(
                schoolId, examinationId, classId, streamId, "STUDENTS", idempotencyKey, 10L);

        assertEquals(1, dto.totalRecipients());
        assertEquals(0, dto.excludedCount());
    }

    @Test
    void listReturnsOnlySchoolNotifications() {
        ResultNotification n1 = new ResultNotification();
        n1.setId(1L);
        n1.setSchoolId(schoolId);
        lenient().when(notificationRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(n1)));

        Page<ResultNotificationDto> page = service.list(schoolId, PageRequest.of(0, 20));
        assertEquals(1, page.getContent().size());
        assertEquals(1L, page.getContent().get(0).id());
    }

    @Test
    void getDeliveriesReturnsDeliveriesForNotification() {
        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(1L);
        delivery.setNotificationId(500L);
        delivery.setRecipientType("STUDENT");
        delivery.setRecipientId(studentId);
        delivery.setRecipientName("John Doe");
        delivery.setStatus("PENDING");

        lenient().when(notificationRepository.findBySchoolIdAndId(schoolId, 500L))
                .thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(500L))
                .thenReturn(List.of(delivery));

        List<com.edumanage.resultsnotifications.dto.ResultNotificationDeliveryDto> deliveries =
                service.getDeliveries(schoolId, 500L);

        assertEquals(1, deliveries.size());
        assertEquals("STUDENT", deliveries.get(0).recipientType());
    }

    @Test
    void getSummaryCountsStatuses() {
        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);

        ResultNotificationDelivery sent = new ResultNotificationDelivery();
        sent.setStatus("SENT");
        ResultNotificationDelivery failed = new ResultNotificationDelivery();
        failed.setStatus("FAILED");
        ResultNotificationDelivery pending = new ResultNotificationDelivery();
        pending.setStatus("PENDING");
        ResultNotificationDelivery excluded = new ResultNotificationDelivery();
        excluded.setStatus("EXCLUDED");

        lenient().when(notificationRepository.findBySchoolIdAndId(schoolId, 500L))
                .thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(500L))
                .thenReturn(List.of(sent, failed, pending, excluded));

        com.edumanage.resultsnotifications.dto.ResultNotificationSummary summary =
                service.getSummary(schoolId, 500L);

        assertEquals(4, summary.totalRecipients());
        assertEquals(1, summary.excludedCount());
        assertEquals(1, summary.successfulCount());
        assertEquals(1, summary.failedCount());
        assertEquals(1, summary.pendingCount());
    }

    @Test
    void retryFailedRetriesEligibleFailedDelivery() {
        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setTitle("Results Notification - Mid-Term Exam - Form 1");

        ResultNotificationDelivery failed = new ResultNotificationDelivery();
        failed.setId(1L);
        failed.setNotificationId(500L);
        failed.setRecipientPhone("0712345678");
        failed.setStatus("FAILED");
        failed.setFailureCode("SMS_PROVIDER_UNAVAILABLE");
        failed.setAttemptCount(1);

        lenient().when(notificationRepository.findBySchoolIdAndId(schoolId, 500L))
                .thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.findByNotificationIdAndStatus(500L, "FAILED"))
                .thenReturn(List.of(failed));
        lenient().when(deliveryRepository.markPendingDeliveryForRetry(1L)).thenReturn(1);

        SmsMessageDto sentDto = new SmsMessageDto();
        sentDto.setId(9001L);
        sentDto.setProviderMessageId("pm-1");
        sentDto.setBatchId("batch-1");
        lenient().when(smsMessageService.sendSms(any(CreateSmsMessageRequest.class))).thenReturn(sentDto);

        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));

        ResultNotificationDto dto = service.retryFailed(schoolId, 500L, 10L);

        assertNotNull(dto);
        assertEquals(500L, dto.id());
        verify(deliveryRepository).markPendingDeliveryForRetry(1L);
        verify(smsMessageService).sendSms(any(CreateSmsMessageRequest.class));
    }

    @Test
    void retryFailedSkipsDeliveryExceedingMaxAttempts() {
        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setTitle("Results Notification - Mid-Term Exam - Form 1");

        ResultNotificationDelivery failed = new ResultNotificationDelivery();
        failed.setId(1L);
        failed.setNotificationId(500L);
        failed.setRecipientPhone("0712345678");
        failed.setStatus("FAILED");
        failed.setFailureCode("SMS_PROVIDER_UNAVAILABLE");
        failed.setAttemptCount(3);

        lenient().when(notificationRepository.findBySchoolIdAndId(schoolId, 500L))
                .thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.findByNotificationIdAndStatus(500L, "FAILED"))
                .thenReturn(List.of(failed));

        ResultNotificationDto dto = service.retryFailed(schoolId, 500L, 10L);

        assertNotNull(dto);
        verify(deliveryRepository, never()).markPendingDeliveryForRetry(anyLong());
        verify(smsMessageService, never()).sendSms(any());
    }

    @Test
    void retryFailedSkipsPermanentFailureCode() {
        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setTitle("Results Notification - Mid-Term Exam - Form 1");

        ResultNotificationDelivery failed = new ResultNotificationDelivery();
        failed.setId(1L);
        failed.setNotificationId(500L);
        failed.setRecipientPhone("0712345678");
        failed.setStatus("FAILED");
        failed.setFailureCode("INVALID_PHONE");
        failed.setAttemptCount(1);

        lenient().when(notificationRepository.findBySchoolIdAndId(schoolId, 500L))
                .thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.findByNotificationIdAndStatus(500L, "FAILED"))
                .thenReturn(List.of(failed));

        ResultNotificationDto dto = service.retryFailed(schoolId, 500L, 10L);

        assertNotNull(dto);
        verify(deliveryRepository, never()).markPendingDeliveryForRetry(anyLong());
        verify(smsMessageService, never()).sendSms(any());
    }

    @Test
    void retryFailedSkipsWhenMarkReturnsZero() {
        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setTitle("Results Notification - Mid-Term Exam - Form 1");

        ResultNotificationDelivery failed = new ResultNotificationDelivery();
        failed.setId(1L);
        failed.setNotificationId(500L);
        failed.setRecipientPhone("0712345678");
        failed.setStatus("FAILED");
        failed.setFailureCode("SMS_PROVIDER_UNAVAILABLE");
        failed.setAttemptCount(1);

        lenient().when(notificationRepository.findBySchoolIdAndId(schoolId, 500L))
                .thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.findByNotificationIdAndStatus(500L, "FAILED"))
                .thenReturn(List.of(failed));
        lenient().when(deliveryRepository.markPendingDeliveryForRetry(1L)).thenReturn(0);

        ResultNotificationDto dto = service.retryFailed(schoolId, 500L, 10L);

        assertNotNull(dto);
        verify(deliveryRepository).markPendingDeliveryForRetry(1L);
        verify(smsMessageService, never()).sendSms(any());
    }

    @Test
    void retryFailedThrowsWhenNotificationNotFound() {
        lenient().when(notificationRepository.findBySchoolIdAndId(schoolId, 999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.retryFailed(schoolId, 999L, 10L));
    }

    /* ============ PART A: delivery status synchronization ============ */

    @Test
    void successfulProviderResponseChangesDeliveryPendingToSent() {
        Long smsMessageId = 9000L;
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setId(smsMessageId);
        smsMessage.setSchoolId(schoolId);
        smsMessage.setStatus("ACCEPTED");
        smsMessage.setProviderMessageId("pm-1");
        smsMessage.setBatchId("batch-1");

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(1L);
        delivery.setNotificationId(500L);
        delivery.setSmsMessageId(smsMessageId);
        delivery.setStatus("PENDING");

        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setStatus("PENDING");

        lenient().when(smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId))
                .thenReturn(Optional.of(smsMessage));
        lenient().when(deliveryRepository.findBySmsMessageId(smsMessageId))
                .thenReturn(Optional.of(delivery));
        lenient().when(deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(500L))
                .thenReturn(List.of(delivery));
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.save(any())).thenReturn(delivery);
        lenient().when(notificationRepository.save(any())).thenReturn(notification);

        ResultNotificationDelivery synced = service.syncDeliveryFromSmsMessage(schoolId, smsMessageId);

        assertEquals("SENT", synced.getStatus());
        assertEquals("pm-1", synced.getProviderMessageId());
        assertEquals("batch-1", synced.getBatchId());
        assertNotNull(synced.getSentAt());
        assertEquals("SENT", notification.getStatus());
        verify(smsMessageService, never()).sendSms(any());
    }

    @Test
    void deliveryReceiptChangesSentToDelivered() {
        Long smsMessageId = 9001L;
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setId(smsMessageId);
        smsMessage.setSchoolId(schoolId);
        smsMessage.setStatus("DELIVERED");

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(2L);
        delivery.setNotificationId(500L);
        delivery.setSmsMessageId(smsMessageId);
        delivery.setStatus("SENT");

        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setStatus("SENT");

        lenient().when(smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId))
                .thenReturn(Optional.of(smsMessage));
        lenient().when(deliveryRepository.findBySmsMessageId(smsMessageId))
                .thenReturn(Optional.of(delivery));
        lenient().when(deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(500L))
                .thenReturn(List.of(delivery));
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.save(any())).thenReturn(delivery);
        lenient().when(notificationRepository.save(any())).thenReturn(notification);

        ResultNotificationDelivery synced = service.syncDeliveryFromSmsMessage(schoolId, smsMessageId);

        assertEquals("DELIVERED", synced.getStatus());
        assertNotNull(synced.getDeliveredAt());
        assertEquals("SENT", notification.getStatus());
        verify(smsMessageService, never()).sendSms(any());
    }

    @Test
    void providerFailureChangesDeliveryToFailed() {
        Long smsMessageId = 9002L;
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setId(smsMessageId);
        smsMessage.setSchoolId(schoolId);
        smsMessage.setStatus("FAILED");
        smsMessage.setErrorMessage("Provider unreachable");

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(3L);
        delivery.setNotificationId(500L);
        delivery.setSmsMessageId(smsMessageId);
        delivery.setStatus("PENDING");

        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setStatus("PENDING");

        lenient().when(smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId))
                .thenReturn(Optional.of(smsMessage));
        lenient().when(deliveryRepository.findBySmsMessageId(smsMessageId))
                .thenReturn(Optional.of(delivery));
        lenient().when(deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(500L))
                .thenReturn(List.of(delivery));
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.save(any())).thenReturn(delivery);
        lenient().when(notificationRepository.save(any())).thenReturn(notification);

        ResultNotificationDelivery synced = service.syncDeliveryFromSmsMessage(schoolId, smsMessageId);

        assertEquals("FAILED", synced.getStatus());
        assertEquals("SMS_PROVIDER_FAILED", synced.getFailureCode());
        assertEquals("Provider unreachable", synced.getErrorMessage());
        assertNotNull(synced.getFailedAt());
        assertEquals("FAILED", notification.getStatus());
    }

    @Test
    void duplicateCallbacksDoNotDuplicateCountsOrCorruptStatus() {
        Long smsMessageId = 9003L;
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setId(smsMessageId);
        smsMessage.setSchoolId(schoolId);
        smsMessage.setStatus("ACCEPTED");

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(4L);
        delivery.setNotificationId(500L);
        delivery.setSmsMessageId(smsMessageId);
        delivery.setStatus("PENDING");

        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setStatus("PENDING");

        lenient().when(smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId))
                .thenReturn(Optional.of(smsMessage));
        lenient().when(deliveryRepository.findBySmsMessageId(smsMessageId))
                .thenReturn(Optional.of(delivery));
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.save(any())).thenReturn(delivery);
        lenient().when(notificationRepository.save(any())).thenReturn(notification);

        ResultNotificationDelivery first = service.syncDeliveryFromSmsMessage(schoolId, smsMessageId);
        ResultNotificationDelivery second = service.syncDeliveryFromSmsMessage(schoolId, smsMessageId);

        assertEquals("SENT", first.getStatus());
        assertEquals("SENT", second.getStatus());
        // The second callback is idempotent: no additional save or status refresh.
        verify(deliveryRepository, times(1)).save(any());
        verify(notificationRepository, times(1)).save(any());
        verify(smsMessageService, never()).sendSms(any());
    }

    @Test
    void notificationAggregateStatusRefreshesToSentAfterDeliverySync() {
        Long smsMessageId = 9004L;
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setId(smsMessageId);
        smsMessage.setSchoolId(schoolId);
        smsMessage.setStatus("ACCEPTED");

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(5L);
        delivery.setNotificationId(500L);
        delivery.setSmsMessageId(smsMessageId);
        delivery.setStatus("PENDING");

        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setStatus("PENDING");

        lenient().when(smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId))
                .thenReturn(Optional.of(smsMessage));
        lenient().when(deliveryRepository.findBySmsMessageId(smsMessageId))
                .thenReturn(Optional.of(delivery));
        lenient().when(deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(500L))
                .thenReturn(List.of(delivery));
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.save(any())).thenReturn(delivery);
        lenient().when(notificationRepository.save(any())).thenReturn(notification);

        service.syncDeliveryFromSmsMessage(schoolId, smsMessageId);

        assertEquals("SENT", notification.getStatus());
    }

    @Test
    void smsSuccessFollowedByDbSyncFailureCanBeReconciledWithoutResendingSms() {
        Long smsMessageId = 9005L;
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setId(smsMessageId);
        smsMessage.setSchoolId(schoolId);
        smsMessage.setStatus("ACCEPTED");
        smsMessage.setProviderMessageId("pm-reconcile");

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(6L);
        delivery.setNotificationId(500L);
        delivery.setSmsMessageId(smsMessageId);
        delivery.setStatus("PENDING"); // stuck: provider accepted but delivery never updated

        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setStatus("PENDING");

        lenient().when(smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId))
                .thenReturn(Optional.of(smsMessage));
        lenient().when(deliveryRepository.findBySmsMessageId(smsMessageId))
                .thenReturn(Optional.of(delivery));
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.save(any())).thenReturn(delivery);
        lenient().when(notificationRepository.save(any())).thenReturn(notification);

        ResultNotificationDeliveryDto reconciled = service.reconcileDelivery(schoolId, smsMessageId);

        assertNotNull(reconciled);
        assertEquals("SENT", reconciled.status());
        assertEquals("pm-reconcile", reconciled.providerMessageId());
        // No SMS is sent during reconciliation.
        verify(smsMessageService, never()).sendSms(any());
    }

    @Test
    void schoolCannotUpdateAnotherSchoolsDelivery() {
        Long otherSchoolId = 2L;
        Long smsMessageId = 9006L;
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setId(smsMessageId);
        smsMessage.setSchoolId(otherSchoolId); // belongs to school B
        smsMessage.setStatus("ACCEPTED");

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(7L);
        delivery.setNotificationId(500L);
        delivery.setSmsMessageId(smsMessageId);
        delivery.setStatus("PENDING");

        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(otherSchoolId); // notification belongs to school B
        notification.setStatus("PENDING");

        lenient().when(smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId))
                .thenReturn(Optional.of(smsMessage));
        lenient().when(deliveryRepository.findBySmsMessageId(smsMessageId))
                .thenReturn(Optional.of(delivery));
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));

        assertThrows(ForbiddenException.class, () ->
                service.syncDeliveryFromSmsMessage(schoolId, smsMessageId));
    }

    @Test
    void reconciliationPreservesRetryLimitsAndDoesNotResend() {
        Long smsMessageId = 9007L;
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setId(smsMessageId);
        smsMessage.setSchoolId(schoolId);
        smsMessage.setStatus("FAILED");

        ResultNotificationDelivery delivery = new ResultNotificationDelivery();
        delivery.setId(8L);
        delivery.setNotificationId(500L);
        delivery.setSmsMessageId(smsMessageId);
        delivery.setStatus("PENDING");
        delivery.setAttemptCount(3); // at retry limit

        ResultNotification notification = new ResultNotification();
        notification.setId(500L);
        notification.setSchoolId(schoolId);
        notification.setStatus("PENDING");

        lenient().when(smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId))
                .thenReturn(Optional.of(smsMessage));
        lenient().when(deliveryRepository.findBySmsMessageId(smsMessageId))
                .thenReturn(Optional.of(delivery));
        lenient().when(notificationRepository.findById(500L)).thenReturn(Optional.of(notification));
        lenient().when(deliveryRepository.save(any())).thenReturn(delivery);
        lenient().when(notificationRepository.save(any())).thenReturn(notification);

        ResultNotificationDelivery synced = service.syncDeliveryFromSmsMessage(schoolId, smsMessageId);

        assertEquals("FAILED", synced.getStatus());
        assertEquals(3, delivery.getAttemptCount()); // retry limit untouched
        verify(smsMessageService, never()).sendSms(any());
    }
}
