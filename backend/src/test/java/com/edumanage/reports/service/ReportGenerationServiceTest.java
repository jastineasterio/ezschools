package com.edumanage.reports.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.attendance.entity.Attendance;
import com.edumanage.attendance.repository.AttendanceRepository;
import com.edumanage.boarding.entity.BoardingStudent;
import com.edumanage.boarding.repository.BoardingStudentRepository;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.departments.entity.Department;
import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.examinationsubjects.entity.ExaminationSubject;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.fees.entity.FeeAssignment;
import com.edumanage.fees.entity.Payment;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.fees.repository.PaymentRepository;
import com.edumanage.reports.dto.CreateReportJobRequest;
import com.edumanage.reports.dto.ReportFilterDefinitionsResponse;
import com.edumanage.reports.dto.ReportJobDto;
import com.edumanage.reports.dto.ReportOptionDto;
import com.edumanage.reports.entity.ReportJob;
import com.edumanage.reports.pdf.ReportPdfRenderer;
import com.edumanage.reports.repository.ReportJobRepository;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    @Mock private ReportJobRepository reportJobRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private com.edumanage.branding.repository.SchoolBrandingRepository brandingRepository;
    @Mock private StudentProfileRepository studentRepository;
    @Mock private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock private com.edumanage.streams.repository.StreamRepository streamRepository;
    @Mock private ClassRepository classRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private ExaminationSubjectRepository examinationSubjectRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private FeeAssignmentRepository feeAssignmentRepository;
    @Mock private TeacherProfileRepository teacherProfileRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private BoardingStudentRepository boardingStudentRepository;
    @Mock private com.edumanage.boarding.repository.DormitoryRepository dormitoryRepository;
    @Mock private com.edumanage.boarding.repository.RoomRepository roomRepository;
    @Mock private com.edumanage.boarding.repository.BedRepository bedRepository;
    @Mock private com.edumanage.sms.repository.SmsMessageRepository smsMessageRepository;
    @Mock private com.edumanage.notifications.repository.CommunicationCampaignRepository communicationCampaignRepository;
    @Mock private com.edumanage.notifications.repository.CommunicationDeliveryRepository communicationDeliveryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReportPdfRenderer pdfRenderer;

    @InjectMocks
    private ReportGenerationService service;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContext.setTenantId(7L);
        CustomUserDetails user = new CustomUserDetails(1L, "manager@test.com", "password", 7L, true,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void definitionsReturnsSupportedReportTypes() {
        ReportFilterDefinitionsResponse defs = service.getDefinitions();
        assertNotNull(defs);
        assertTrue(defs.getReportTypes().size() >= 7);
        var ids = defs.getReportTypes().stream().map(com.edumanage.reports.dto.ReportTypeInfoDto::getId).toList();
        assertTrue(ids.contains("STUDENT_DIRECTORY"));
        assertTrue(ids.contains("ATTENDANCE_BY_DATE_RANGE"));
        assertTrue(ids.contains("EXAMINATION_RESULTS"));
        assertTrue(ids.contains("PAYMENTS_REPORT"));
        assertTrue(ids.contains("TEACHER_DIRECTORY"));
        assertTrue(ids.contains("BOARDING_STUDENTS"));
        assertTrue(ids.contains("SMS_HISTORY"));
    }

    @Test
    void definitionsStudentReportHasDynamicFields() {
        ReportFilterDefinitionsResponse defs = service.getDefinitions();
        var student = defs.getReportTypes().stream()
                .filter(t -> "STUDENT_DIRECTORY".equals(t.getId())).findFirst().orElseThrow();
        var fieldNames = student.getFields().stream().map(com.edumanage.reports.dto.ReportFilterFieldDto::getName).toList();
        assertTrue(fieldNames.contains("academicYear"));
        assertTrue(fieldNames.contains("class"));
        assertTrue(fieldNames.contains("stream"));
        assertTrue(fieldNames.contains("gender"));
        assertTrue(fieldNames.contains("status"));
        assertTrue(fieldNames.contains("includeFields"));
        // stream depends on class
        var streamField = student.getFields().stream()
                .filter(f -> "stream".equals(f.getName())).findFirst().orElseThrow();
        assertEquals("class", streamField.getDependsOn());
    }

    @Test
    void optionsClassesReturnsSchoolClasses() {
        Class c = new Class();
        c.setId(10L); c.setSchoolId(7L); c.setName("Grade 10"); c.setCode("G10");
        when(classRepository.findBySchoolId(eq(7L), any())).thenReturn(new PageImpl<>(List.of(c)));
        List<ReportOptionDto> opts = service.getOptions("classes", null, null, null);
        assertEquals(1, opts.size());
        assertEquals("10", opts.get(0).getId());
        assertEquals("Grade 10", opts.get(0).getName());
    }

    @Test
    void optionsStreamsDependentOnClass() {
        com.edumanage.streams.entity.Stream s = new com.edumanage.streams.entity.Stream();
        s.setId(20L); s.setSchoolId(7L); s.setClassId(10L); s.setName("Science"); s.setCode("SCI");
        when(streamRepository.findByClassIdAndSchoolId(10L, 7L)).thenReturn(List.of(s));
        List<ReportOptionDto> opts = service.getOptions("streams", 10L, null, null);
        assertEquals(1, opts.size());
        assertEquals("20", opts.get(0).getId());
        assertEquals("Science", opts.get(0).getName());
    }

    @Test
    void optionsStreamsEmptyWithoutClass() {
        List<ReportOptionDto> opts = service.getOptions("streams", null, null, null);
        assertTrue(opts.isEmpty());
    }

    @Test
    void optionsAcademicYears() {
        AcademicYear ay = new AcademicYear();
        ay.setId(30L); ay.setSchoolId(7L); ay.setName("2026/2027");
        when(academicYearRepository.findBySchoolIdOrderByStartDateDesc(7L)).thenReturn(List.of(ay));
        List<ReportOptionDto> opts = service.getOptions("academicYears", null, null, null);
        assertEquals(1, opts.size());
        assertEquals("30", opts.get(0).getId());
    }

    @Test
    void optionsExaminationsByAcademicYear() {
        Examination e = new Examination();
        e.setId(40L); e.setSchoolId(7L); e.setAcademicYearId(30L); e.setName("Mid Term");
        when(examinationRepository.findBySchoolIdAndAcademicYearId(eq(7L), eq(30L), any()))
                .thenReturn(new PageImpl<>(List.of(e)));
        List<ReportOptionDto> opts = service.getOptions("examinations", null, 30L, null);
        assertEquals(1, opts.size());
        assertEquals("40", opts.get(0).getId());
    }

    @Test
    void optionsStudentsFilteredByClass() {
        StudentProfile s = new StudentProfile();
        s.setId(50L); s.setSchoolId(7L); s.setAdmissionNumber("A001");
        s.setFirstName("Alice"); s.setLastName("Smith");
        when(studentRepository.findBySchoolIdAndDeletedAtIsNull(eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of(s)));
        com.edumanage.enrollments.entity.StudentEnrollment e = new com.edumanage.enrollments.entity.StudentEnrollment();
        e.setClassId(10L);
        when(studentEnrollmentRepository.findByStudentIdAndSchoolId(50L, 7L)).thenReturn(List.of(e));
        List<ReportOptionDto> opts = service.getOptions("students", 10L, null, null);
        assertEquals(1, opts.size());
        assertEquals("50", opts.get(0).getId());
    }

    @Test
    void validateRejectsDateToBeforeDateFrom() {
        CreateReportJobRequest req = new CreateReportJobRequest();
        req.setReportType("ATTENDANCE_BY_DATE_RANGE");
        req.setReportName("Test");
        req.setDateFrom(LocalDate.of(2026, 2, 1));
        req.setDateTo(LocalDate.of(2026, 1, 1));
        assertThrows(ForbiddenException.class, () -> service.generate(req));
    }

    @Test
    void generateRejectsCrossSchoolClass() {
        CreateReportJobRequest req = new CreateReportJobRequest();
        req.setReportType("STUDENT_DIRECTORY");
        req.setReportName("Test");
        req.setClassId(99L);
        when(classRepository.findBySchoolIdAndId(7L, 99L)).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class, () -> service.generate(req));
    }

    @Test
    void generateRejectsStreamNotInClass() {
        CreateReportJobRequest req = new CreateReportJobRequest();
        req.setReportType("STUDENT_DIRECTORY");
        req.setReportName("Test");
        req.setClassId(10L);
        req.setStreamId(20L);
        when(classRepository.findBySchoolIdAndId(7L, 10L)).thenReturn(Optional.of(new Class()));
        var stream = new com.edumanage.streams.entity.Stream();
        stream.setId(20L); stream.setClassId(99L); // different class
        when(streamRepository.findBySchoolIdAndId(7L, 20L)).thenReturn(Optional.of(stream));
        assertThrows(ForbiddenException.class, () -> service.generate(req));
    }

    @Test
    void getReportRejectsCrossSchoolAccess() {
        when(reportJobRepository.findByIdAndSchoolId(1L, 7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getReport(1L));
    }

    @Test
    void generateCreatesPendingJobAndMarksCompleted() throws Exception {
        CreateReportJobRequest req = new CreateReportJobRequest();
        req.setReportType("STUDENT_DIRECTORY");
        req.setReportName("Student Directory");
        req.setAcademicYearId(30L);

        School school = new School();
        school.setId(7L); school.setName("Test School"); school.setAddress("Addr");
        school.setPhone("+1 555"); school.setEmail("a@b.com"); school.setRegistrationNumber("REG1");

        when(schoolRepository.findById(7L)).thenReturn(Optional.of(school));
        when(brandingRepository.findBySchoolId(7L)).thenReturn(java.util.Optional.empty());
        when(academicYearRepository.findBySchoolIdAndId(7L, 30L)).thenReturn(Optional.of(new AcademicYear()));
        when(studentRepository.findBySchoolIdAndDeletedAtIsNull(eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(pdfRenderer.render(any())).thenReturn(new byte[]{37, 80, 68, 70}); // %PDF

        ReportJob saved = new ReportJob();
        saved.setId(1L); saved.setSchoolId(7L); saved.setStatus("COMPLETED");
        saved.setFileName("Student_Directory_1.pdf");
        when(reportJobRepository.save(any(ReportJob.class))).thenAnswer(inv -> {
            ReportJob j = inv.getArgument(0);
            j.setId(1L);
            return j;
        });
        when(reportJobRepository.findById(1L)).thenReturn(Optional.of(saved));

        ReportJobDto dto = service.generate(req);
        assertNotNull(dto);
        assertEquals("COMPLETED", dto.getStatus());
    }

    @Test
    void unicodeKiswahiliRendersInPdf() throws Exception {
        ReportPdfRenderer renderer = new ReportPdfRenderer();
        com.edumanage.reports.pdf.ReportRenderData data = new com.edumanage.reports.pdf.ReportRenderData();
        data.setSchoolName("Shule ya Msingi");
        data.setReportName("Ripoti ya Wanafunzi");
        data.setGeneratedAt("03 Nov 2026 10:00");
        data.setColumns(List.of("Jina", "Namba ya Serikali"));
        data.setRows(List.of(List.of("Juma Mwakasimba", "A001")));
        byte[] pdf = renderer.render(data);
        assertNotNull(pdf);
        assertTrue(new String(pdf, 0, 5).startsWith("%PDF"));
    }
}
