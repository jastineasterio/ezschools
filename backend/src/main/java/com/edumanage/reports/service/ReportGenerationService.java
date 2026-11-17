package com.edumanage.reports.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.attendance.entity.Attendance;
import com.edumanage.attendance.repository.AttendanceRepository;
import com.edumanage.boarding.entity.BoardingStudent;
import com.edumanage.boarding.repository.BedRepository;
import com.edumanage.boarding.repository.BoardingStudentRepository;
import com.edumanage.boarding.repository.DormitoryRepository;
import com.edumanage.boarding.repository.RoomRepository;
import com.edumanage.boarding.entity.Dormitory;
import com.edumanage.boarding.entity.Room;
import com.edumanage.boarding.entity.Bed;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
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
import com.edumanage.reports.dto.ReportFilterFieldDto;
import com.edumanage.reports.dto.ReportOptionDto;
import com.edumanage.reports.dto.ReportTypeInfoDto;
import com.edumanage.reports.entity.ReportJob;
import com.edumanage.reports.pdf.ReportPdfRenderer;
import com.edumanage.reports.pdf.ReportRenderData;
import com.edumanage.reports.repository.ReportJobRepository;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.sms.entity.SmsMessage;
import com.edumanage.sms.repository.SmsMessageRepository;
import com.edumanage.notifications.repository.CommunicationCampaignRepository;
import com.edumanage.notifications.repository.CommunicationDeliveryRepository;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Service
public class ReportGenerationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final ReportJobRepository reportJobRepository;
    private final SchoolRepository schoolRepository;
    private final com.edumanage.branding.repository.SchoolBrandingRepository brandingRepository;
    private final StudentProfileRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ClassRepository classRepository;
    private final com.edumanage.streams.repository.StreamRepository streamRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ExaminationRepository examinationRepository;
    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final BoardingStudentRepository boardingStudentRepository;
    private final DormitoryRepository dormitoryRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final SmsMessageRepository smsMessageRepository;
    private final CommunicationCampaignRepository communicationCampaignRepository;
    private final CommunicationDeliveryRepository communicationDeliveryRepository;
    private final UserRepository userRepository;
    private final ReportPdfRenderer pdfRenderer;
    @Value("${app.reports.storage-path:data/reports}")
    private String storagePathValue;
    private final Path storageRoot;

    public ReportGenerationService(ReportJobRepository reportJobRepository,
                                     SchoolRepository schoolRepository,
                                     com.edumanage.branding.repository.SchoolBrandingRepository brandingRepository,
                                     StudentProfileRepository studentRepository,
                                     StudentEnrollmentRepository studentEnrollmentRepository,
                                     ClassRepository classRepository,
                                     com.edumanage.streams.repository.StreamRepository streamRepository,
                                     DepartmentRepository departmentRepository,
                                     SubjectRepository subjectRepository,
                                     AcademicYearRepository academicYearRepository,
                                     ExaminationRepository examinationRepository,
                                     ExaminationSubjectRepository examinationSubjectRepository,
                                     ResultRepository resultRepository,
                                     AttendanceRepository attendanceRepository,
                                     PaymentRepository paymentRepository,
                                     FeeAssignmentRepository feeAssignmentRepository,
                                     TeacherProfileRepository teacherProfileRepository,
                                     TeacherAssignmentRepository teacherAssignmentRepository,
                                     BoardingStudentRepository boardingStudentRepository,
                                     DormitoryRepository dormitoryRepository,
                                     RoomRepository roomRepository,
                                     BedRepository bedRepository,
                                     SmsMessageRepository smsMessageRepository,
                                     CommunicationCampaignRepository communicationCampaignRepository,
                                     CommunicationDeliveryRepository communicationDeliveryRepository,
                                     UserRepository userRepository,
                                     ReportPdfRenderer pdfRenderer) {
        this.reportJobRepository = reportJobRepository;
        this.schoolRepository = schoolRepository;
        this.brandingRepository = brandingRepository;
        this.studentRepository = studentRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.academicYearRepository = academicYearRepository;
        this.examinationRepository = examinationRepository;
        this.examinationSubjectRepository = examinationSubjectRepository;
        this.resultRepository = resultRepository;
        this.attendanceRepository = attendanceRepository;
        this.paymentRepository = paymentRepository;
        this.feeAssignmentRepository = feeAssignmentRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.boardingStudentRepository = boardingStudentRepository;
        this.dormitoryRepository = dormitoryRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.smsMessageRepository = smsMessageRepository;
        this.communicationCampaignRepository = communicationCampaignRepository;
        this.communicationDeliveryRepository = communicationDeliveryRepository;
        this.userRepository = userRepository;
        this.pdfRenderer = pdfRenderer;
        this.storageRoot = Paths.get(storagePathValue != null ? storagePathValue : "data/reports")
                .toAbsolutePath().normalize();
    }

    @Transactional
    public com.edumanage.reports.dto.ReportJobDto generate(CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        Long userId = currentUserId();

        validateReportType(request.getReportType());
        validateFilters(request, schoolId);

        ReportJob job = new ReportJob();
        job.setSchoolId(schoolId);
        job.setRequestedBy(userId);
        job.setReportType(request.getReportType());
        job.setReportName(request.getReportName());
        job.setParameters(serializeParameters(request));
        job.setStatus("PENDING");
        job.setExpiresAt(LocalDateTime.now().plusDays(7));
        reportJobRepository.save(job);

        try {
            updateStatus(job.getId(), "PROCESSING", null, null, null, null);

            School school = schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("School not found"));
            var branding = brandingRepository.findBySchoolId(schoolId).orElse(null);

            ReportRenderData data = buildRenderData(request, school, branding);
            byte[] pdf = pdfRenderer.render(data);

            StoredFile stored = storePdf(schoolId, job.getId(), request.getReportName(), pdf);

            updateStatus(job.getId(), "COMPLETED", stored.fileUrl, stored.fileName, stored.fileSize, null);
            return toDto(reportJobRepository.findById(job.getId()).orElseThrow());
        } catch (Exception e) {
            updateStatus(job.getId(), "FAILED", null, null, null, e.getMessage());
            throw new RuntimeException("Report generation failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public ReportFilterDefinitionsResponse getDefinitions() {
        ReportFilterDefinitionsResponse response = new ReportFilterDefinitionsResponse();
        response.setReportTypes(buildReportTypes());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ReportOptionDto> getOptions(String kind, Long classId, Long academicYearId, Long departmentId) {
        Long schoolId = TenantContext.requireTenantId();
        switch (kind) {
            case "academicYears":
                return toOptions(academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId));
            case "classes":
                return toOptions(classRepository.findBySchoolId(schoolId, PageRequest.of(0, 200)).getContent());
            case "streams":
                if (classId == null) {
                    return List.of();
                }
                return toOptions(streamRepository.findByClassIdAndSchoolId(classId, schoolId));
            case "departments":
                return toOptions(departmentRepository.findBySchoolId(schoolId, PageRequest.of(0, 200)).getContent());
            case "subjects":
                List<Subject> subjects = subjectRepository.findBySchoolId(schoolId, PageRequest.of(0, 200)).getContent();
                if (departmentId != null) {
                    subjects = subjects.stream().filter(s -> departmentId.equals(s.getDepartmentId())).collect(Collectors.toList());
                }
                return toOptions(subjects);
            case "examinations":
                List<Examination> exams = academicYearId == null
                        ? examinationRepository.findBySchoolId(schoolId, PageRequest.of(0, 200)).getContent()
                        : examinationRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId,
                        PageRequest.of(0, 200)).getContent();
                return toOptions(exams);
            case "students":
                List<StudentProfile> students = studentRepository.findBySchoolIdAndDeletedAtIsNull(schoolId,
                        PageRequest.of(0, 500)).getContent();
                if (classId != null) {
                    students = students.stream().filter(s -> classId.equals(classIdOfStudent(s.getId()))).collect(Collectors.toList());
                }
                return toOptions(students);
            case "statuses":
                return staticOptions("ACTIVE", "INACTIVE", "GRADUATED", "TRANSFERRED", "SUSPENDED");
            case "teacherStatuses":
                return staticOptions("ACTIVE", "INACTIVE", "ON_LEAVE", "RESIGNED", "TERMINATED");
            case "genders":
                return staticOptions("MALE", "FEMALE", "OTHER");
            case "attendanceStatuses":
                return staticOptions("PRESENT", "ABSENT", "LATE", "EXCUSED");
            case "feeStatuses":
                return staticOptions("PENDING", "PARTIAL", "PAID", "OVERDUE");
            case "boardingStatuses":
                return staticOptions("ACTIVE", "RELEASED", "SUSPENDED");
            case "smsStatuses":
                return staticOptions("SENT", "DELIVERED", "FAILED", "PENDING");
            default:
                throw new ForbiddenException("Unknown filter option kind: " + kind);
        }
    }

    @Transactional(readOnly = true)
    public com.edumanage.reports.dto.ReportJobDto getReport(Long jobId) {
        Long schoolId = TenantContext.requireTenantId();
        ReportJob job = reportJobRepository.findByIdAndSchoolId(jobId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return toDto(job);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> downloadPdf(Long jobId) throws IOException {
        Long schoolId = TenantContext.requireTenantId();
        ReportJob job = reportJobRepository.findByIdAndSchoolId(jobId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        if (!"COMPLETED".equals(job.getStatus()) || job.getFileName() == null) {
            throw new ResourceNotFoundException("Report PDF is not available");
        }
        Path file = storageRoot.resolve(schoolId.toString()).resolve(job.getId() + ".pdf").normalize();
        if (!Files.isReadable(file)) {
            throw new ResourceNotFoundException("Report PDF file not found");
        }
        String mime = URLConnection.guessContentTypeFromName(file.getFileName().toString());
        if (mime == null) {
            mime = "application/pdf";
        }
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file.toFile()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(file)))
                .body(resource);
    }

    @Transactional
    public com.edumanage.reports.dto.ReportJobDto regenerate(Long jobId) {
        Long schoolId = TenantContext.requireTenantId();
        ReportJob job = reportJobRepository.findByIdAndSchoolId(jobId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        CreateReportJobRequest request = new CreateReportJobRequest();
        request.setReportType(job.getReportType());
        request.setReportName(job.getReportName());
        request.setParameters(job.getParameters());

        job.setStatus("PENDING");
        job.setErrorMessage(null);
        job.setFileUrl(null);
        job.setFileName(null);
        job.setFileSize(null);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        reportJobRepository.save(job);

        try {
            updateStatus(job.getId(), "PROCESSING", null, null, null, null);
            School school = schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("School not found"));
            var branding = brandingRepository.findBySchoolId(schoolId).orElse(null);
            ReportRenderData data = buildRenderData(request, school, branding);
            byte[] pdf = pdfRenderer.render(data);
            StoredFile stored = storePdf(schoolId, job.getId(), request.getReportName(), pdf);
            updateStatus(job.getId(), "COMPLETED", stored.fileUrl, stored.fileName, stored.fileSize, null);
            return toDto(reportJobRepository.findById(job.getId()).orElseThrow());
        } catch (Exception e) {
            updateStatus(job.getId(), "FAILED", null, null, null, e.getMessage());
            throw new RuntimeException("Report regeneration failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteReport(Long jobId) {
        Long schoolId = TenantContext.requireTenantId();
        ReportJob job = reportJobRepository.findByIdAndSchoolId(jobId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        try {
            Path file = storageRoot.resolve(schoolId.toString()).resolve(job.getId() + ".pdf").normalize();
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
        reportJobRepository.delete(job);
    }

    /* ---------------------------------------------------------------- */

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).id();
        }
        throw new ForbiddenException("User not authenticated");
    }

    private void validateReportType(String reportType) {
        if (reportType == null || !SUPPORTED_TYPES.contains(reportType)) {
            throw new ForbiddenException("Unsupported report type: " + reportType);
        }
    }

    private void validateFilters(CreateReportJobRequest request, Long schoolId) {
        if (request.getDateFrom() != null && request.getDateTo() != null
                && request.getDateTo().isBefore(request.getDateFrom())) {
            throw new ForbiddenException("Date To must be on or after Date From");
        }
        if (request.getAcademicYearId() != null) {
            academicYearRepository.findBySchoolIdAndId(schoolId, request.getAcademicYearId())
                    .orElseThrow(() -> new ForbiddenException("Academic year not found in this school"));
        }
        if (request.getClassId() != null) {
            classRepository.findBySchoolIdAndId(schoolId, request.getClassId())
                    .orElseThrow(() -> new ForbiddenException("Class not found in this school"));
        }
        if (request.getStreamId() != null) {
            streamRepository.findBySchoolIdAndId(schoolId, request.getStreamId())
                    .orElseThrow(() -> new ForbiddenException("Stream not found in this school"));
            if (request.getClassId() != null) {
                var stream = streamRepository.findBySchoolIdAndId(schoolId, request.getStreamId()).orElseThrow();
                if (!request.getClassId().equals(stream.getClassId())) {
                    throw new ForbiddenException("Stream does not belong to the selected class");
                }
            }
        }
        if (request.getDepartmentId() != null) {
            departmentRepository.findBySchoolIdAndId(schoolId, request.getDepartmentId())
                    .orElseThrow(() -> new ForbiddenException("Department not found in this school"));
        }
        if (request.getSubjectId() != null) {
            subjectRepository.findBySchoolIdAndId(schoolId, request.getSubjectId())
                    .orElseThrow(() -> new ForbiddenException("Subject not found in this school"));
        }
        if (request.getExaminationId() != null) {
            examinationRepository.findBySchoolIdAndId(schoolId, request.getExaminationId())
                    .orElseThrow(() -> new ForbiddenException("Examination not found in this school"));
        }
        if (request.getStudentId() != null) {
            studentRepository.findBySchoolIdAndId(schoolId, request.getStudentId())
                    .orElseThrow(() -> new ForbiddenException("Student not found in this school"));
        }
    }

    private String serializeParameters(CreateReportJobRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reportType", request.getReportType());
        map.put("reportName", request.getReportName());
        map.put("academicYearId", request.getAcademicYearId());
        map.put("dateFrom", request.getDateFrom() == null ? null : request.getDateFrom().toString());
        map.put("dateTo", request.getDateTo() == null ? null : request.getDateTo().toString());
        map.put("classId", request.getClassId());
        map.put("streamId", request.getStreamId());
        map.put("departmentId", request.getDepartmentId());
        map.put("subjectId", request.getSubjectId());
        map.put("examinationId", request.getExaminationId());
        map.put("studentId", request.getStudentId());
        map.put("status", request.getStatus());
        map.put("gender", request.getGender());
        map.put("includeFields", request.getIncludeFields());
        return new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(map).toString();
    }

    /* ---------------------------------------------------------------- */

    private ReportRenderData buildRenderData(CreateReportJobRequest request, School school,
                                             com.edumanage.branding.entity.SchoolBranding branding) {
        String type = request.getReportType();
        ReportRenderData data = new ReportRenderData();
        data.setSchoolName(school.getName());
        data.setSchoolAddress(school.getAddress());
        data.setSchoolPhone(school.getPhone());
        data.setSchoolEmail(school.getEmail());
        data.setSchoolWebsite(null);
        data.setSchoolRegistrationNumber(school.getRegistrationNumber());
        data.setPrimaryColor(branding == null ? "#3b82f6" : branding.getPrimaryColor());
        data.setSecondaryColor(branding == null ? "#64748b" : branding.getSecondaryColor());
        data.setLogoUrl(branding == null ? null : branding.getLogoUrl());
        data.setReportTypeLabel(typeLabel(type));
        data.setReportName(request.getReportName());
        data.setAcademicYear(nameOfAcademicYear(request.getAcademicYearId()));
        data.setTerm(null);
        data.setClassName(nameOfClass(request.getClassId()));
        data.setStreamName(nameOfStream(request.getStreamId()));
        data.setSubjectName(nameOfSubject(request.getSubjectId()));
        data.setExaminationName(nameOfExamination(request.getExaminationId()));
        data.setReportingPeriod(periodLabel(request));
        data.setGeneratedAt(DATE_TIME_FMT.format(LocalDateTime.now()));
        data.setLandscape(WIDE_TYPES.contains(type));

        switch (type) {
            case "STUDENT_DIRECTORY":
            case "STUDENTS_BY_CLASS":
            case "STUDENTS_BY_STREAM":
            case "STUDENT_ENROLLMENT_REPORT":
            case "STUDENT_ADMISSION_REPORT":
            case "STUDENT_STATUS_REPORT":
                buildStudentReport(data, request);
                break;
            case "CLASS_REPORT":
            case "STREAM_REPORT":
            case "SUBJECT_REPORT":
            case "TEACHING_ASSIGNMENTS_REPORT":
            case "ACADEMIC_YEAR_REPORT":
            case "TIMETABLE_REPORT":
            case "DEPARTMENT_REPORT":
                buildAcademicReport(data, request);
                break;
            case "DAILY_ATTENDANCE":
            case "ATTENDANCE_BY_DATE_RANGE":
            case "ATTENDANCE_BY_CLASS":
            case "ATTENDANCE_BY_STREAM":
            case "INDIVIDUAL_STUDENT_ATTENDANCE":
            case "ATTENDANCE_SUMMARY":
                buildAttendanceReport(data, request);
                break;
            case "EXAMINATION_RESULTS":
            case "RESULTS_BY_CLASS":
            case "RESULTS_BY_STREAM":
            case "RESULTS_BY_SUBJECT":
            case "INDIVIDUAL_STUDENT_RESULTS":
            case "EXAMINATION_PERFORMANCE_SUMMARY":
            case "GRADE_DISTRIBUTION":
            case "SUBJECT_PERFORMANCE_REPORT":
                buildResultsReport(data, request);
                break;
            case "TEACHER_DIRECTORY":
            case "TEACHER_DETAILS":
            case "TEACHERS_BY_DEPARTMENT":
            case "TEACHING_ASSIGNMENTS":
                buildTeacherReport(data, request);
                break;
            case "PAYMENTS_REPORT":
            case "OUTSTANDING_FEES_REPORT":
            case "FEE_COLLECTION_SUMMARY":
            case "PAYMENT_HISTORY":
            case "FINANCIAL_TRANSACTIONS_REPORT":
                buildFinanceReport(data, request);
                break;
            case "BOARDING_STUDENTS":
            case "BOARDING_ALLOCATIONS":
            case "BOARDING_OCCUPANCY":
            case "BOARDING_STATUS_REPORT":
                buildBoardingReport(data, request);
                break;
            case "SMS_HISTORY":
            case "COMMUNICATION_CAMPAIGN_REPORT":
            case "NOTIFICATION_HISTORY":
            case "DELIVERY_STATUS_SUMMARY":
                buildCommunicationReport(data, request);
                break;
            default:
                throw new ForbiddenException("Unsupported report type: " + type);
        }
        return data;
    }

    /* ------------------------- student ------------------------- */
    private void buildStudentReport(ReportRenderData data, CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        List<StudentProfile> students = studentRepository.findBySchoolIdAndDeletedAtIsNull(schoolId,
                PageRequest.of(0, 2000)).getContent();
        students = filterStudents(students, request);

        List<String> include = request.getIncludeFields() == null
                ? List.of("admissionNumber", "fullName", "gender", "dateOfBirth", "class", "stream", "status")
                : request.getIncludeFields();
        List<String> columns = studentColumns(include);
        List<List<String>> rows = new ArrayList<>();
        for (StudentProfile s : students) {
            rows.add(studentRow(s, include));
        }
        data.setColumns(columns);
        data.setRows(rows);
        data.setSummaryStats(studentSummary(students));
    }

    private List<StudentProfile> filterStudents(List<StudentProfile> students, CreateReportJobRequest request) {
        return students.stream()
                .filter(s -> request.getClassId() == null || request.getClassId().equals(classIdOfStudent(s.getId())))
                .filter(s -> request.getStreamId() == null || request.getStreamId().equals(streamIdOfStudent(s.getId())))
                .filter(s -> request.getGender() == null || request.getGender().equalsIgnoreCase(s.getGender()))
                .filter(s -> request.getStatus() == null || request.getStatus().equalsIgnoreCase(s.getStatus()))
                .filter(s -> request.getStudentId() == null || request.getStudentId().equals(s.getId()))
                .sorted(Comparator.comparing(StudentProfile::getAdmissionNumber))
                .collect(Collectors.toList());
    }

    private List<String> studentColumns(List<String> include) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("admissionNumber", "Admission No");
        labels.put("fullName", "Full Name");
        labels.put("gender", "Gender");
        labels.put("dateOfBirth", "Date of Birth");
        labels.put("class", "Class");
        labels.put("stream", "Stream");
        labels.put("parentGuardian", "Parent/Guardian");
        labels.put("contact", "Contact");
        labels.put("enrollmentStatus", "Status");
        List<String> columns = new ArrayList<>();
        for (String key : include) {
            if (labels.containsKey(key)) {
                columns.add(labels.get(key));
            }
        }
        if (columns.isEmpty()) {
            columns.addAll(labels.values());
        }
        return columns;
    }

    private List<String> studentRow(StudentProfile s, List<String> include) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("admissionNumber", s.getAdmissionNumber());
        values.put("fullName", fullName(s));
        values.put("gender", s.getGender() == null ? "-" : s.getGender());
        values.put("dateOfBirth", s.getDateOfBirth() == null ? "-" : s.getDateOfBirth().format(DATE_FMT));
        values.put("class", nameOfClass(classIdOfStudent(s.getId())));
        values.put("stream", nameOfStream(streamIdOfStudent(s.getId())));
        values.put("parentGuardian", s.getEmergencyContactName() == null ? "-" : s.getEmergencyContactName());
        values.put("contact", s.getEmergencyContactPhone() == null ? "-" : s.getEmergencyContactPhone());
        values.put("enrollmentStatus", s.getStatus() == null ? "-" : s.getStatus());
        List<String> row = new ArrayList<>();
        for (String key : include) {
            row.add(values.getOrDefault(key, "-"));
        }
        if (row.isEmpty()) {
            row.addAll(values.values());
        }
        return row;
    }

    private List<String[]> studentSummary(List<StudentProfile> students) {
        long total = students.size();
        long active = students.stream().filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus())).count();
        long male = students.stream().filter(s -> "MALE".equalsIgnoreCase(s.getGender())).count();
        long female = students.stream().filter(s -> "FEMALE".equalsIgnoreCase(s.getGender())).count();
        return Arrays.asList(
                new String[] { "Total Students", String.valueOf(total) },
                new String[] { "Active", String.valueOf(active) },
                new String[] { "Male", String.valueOf(male) },
                new String[] { "Female", String.valueOf(female) }
        );
    }

    /* ------------------------- academic ------------------------- */
    private void buildAcademicReport(ReportRenderData data, CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        String type = request.getReportType();
        List<String> columns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        List<String[]> stats = new ArrayList<>();

        switch (type) {
            case "CLASS_REPORT": {
                List<Class> classes = classRepository.findBySchoolId(schoolId, PageRequest.of(0, 200)).getContent();
                columns = List.of("Class", "Code", "Status", "Students");
                for (Class c : classes) {
                    long count = studentCountByClass(c.getId());
                    rows.add(List.of(c.getName(), c.getCode(), c.getStatus(), String.valueOf(count)));
                }
                stats.add(new String[] { "Total Classes", String.valueOf(classes.size()) });
                break;
            }
            case "STREAM_REPORT": {
                List<com.edumanage.streams.entity.Stream> streams = streamRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, 200)).getContent();
                columns = List.of("Class", "Stream", "Code", "Capacity");
                for (com.edumanage.streams.entity.Stream s : streams) {
                    rows.add(List.of(nameOfClass(s.getClassId()), s.getName(), s.getCode(),
                            s.getCapacity() == null ? "-" : String.valueOf(s.getCapacity())));
                }
                stats.add(new String[] { "Total Streams", String.valueOf(streams.size()) });
                break;
            }
            case "DEPARTMENT_REPORT": {
                List<com.edumanage.departments.entity.Department> depts = departmentRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, 200)).getContent();
                columns = List.of("Department", "Code", "Head of Department", "Status");
                for (com.edumanage.departments.entity.Department d : depts) {
                    rows.add(List.of(d.getName(), d.getCode(), nameOfUser(d.getHeadOfDepartmentId()), d.getStatus()));
                }
                stats.add(new String[] { "Total Departments", String.valueOf(depts.size()) });
                break;
            }
            case "SUBJECT_REPORT": {
                List<Subject> subjects = subjectRepository.findBySchoolId(schoolId, PageRequest.of(0, 200)).getContent();
                columns = List.of("Subject", "Code", "Department", "Status");
                for (Subject s : subjects) {
                    rows.add(List.of(s.getName(), s.getCode(), nameOfDepartment(s.getDepartmentId()), s.getStatus()));
                }
                stats.add(new String[] { "Total Subjects", String.valueOf(subjects.size()) });
                break;
            }
            case "TEACHING_ASSIGNMENTS_REPORT": {
                List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, 500)).getContent();
                columns = List.of("Teacher", "Subject", "Class", "Stream", "Academic Year", "Class Teacher");
                for (TeacherAssignment a : assignments) {
                    rows.add(List.of(nameOfUser(a.getTeacherId()), nameOfSubject(a.getSubjectId()),
                            nameOfClass(a.getClassId()), nameOfStream(a.getStreamId()),
                            nameOfAcademicYear(a.getAcademicYearId()),
                            a.isClassTeacher() ? "Yes" : "No"));
                }
                stats.add(new String[] { "Total Assignments", String.valueOf(assignments.size()) });
                break;
            }
            case "ACADEMIC_YEAR_REPORT": {
                List<AcademicYear> years = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId);
                columns = List.of("Academic Year", "Start Date", "End Date", "Status", "Current");
                for (AcademicYear y : years) {
                    rows.add(List.of(y.getName(), y.getStartDate().format(DATE_FMT), y.getEndDate().format(DATE_FMT),
                            y.getStatus(), y.isCurrent() ? "Yes" : "No"));
                }
                stats.add(new String[] { "Total Academic Years", String.valueOf(years.size()) });
                break;
            }
            case "TIMETABLE_REPORT": {
                List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, 500)).getContent();
                columns = List.of("Teacher", "Subject", "Class", "Stream", "Academic Year");
                for (TeacherAssignment a : assignments) {
                    rows.add(List.of(nameOfUser(a.getTeacherId()), nameOfSubject(a.getSubjectId()),
                            nameOfClass(a.getClassId()), nameOfStream(a.getStreamId()),
                            nameOfAcademicYear(a.getAcademicYearId())));
                }
                stats.add(new String[] { "Total Timetable Entries", String.valueOf(assignments.size()) });
                break;
            }
        }
        data.setColumns(columns);
        data.setRows(rows);
        data.setSummaryStats(stats);
    }

    /* ------------------------- attendance ------------------------- */
    private void buildAttendanceReport(ReportRenderData data, CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        String type = request.getReportType();
        LocalDate from = request.getDateFrom();
        LocalDate to = request.getDateTo();

        List<Attendance> records;
        if ("DAILY_ATTENDANCE".equals(type)) {
            LocalDate d = from != null ? from : LocalDate.now();
            records = attendanceRepository.findBySchoolIdAndAttendanceDate(schoolId, d);
            data.setReportingPeriod(d.format(DATE_FMT));
        } else if ("INDIVIDUAL_STUDENT_ATTENDANCE".equals(type)) {
            records = request.getStudentId() == null ? List.of()
                    : attendanceRepository.findByStudentIdAndSchoolId(request.getStudentId(), schoolId);
            records = filterByDate(records, from, to);
            data.setReportingPeriod(periodLabel(request));
        } else {
            records = attendanceRepository.findBySchoolIdAndDeletedAtIsNull(schoolId,
                    PageRequest.of(0, 5000)).getContent();
            records = records.stream()
                    .filter(a -> request.getClassId() == null || request.getClassId().equals(a.getClassId()))
                    .filter(a -> request.getStreamId() == null || request.getStreamId().equals(a.getStreamId()))
                    .filter(a -> request.getStatus() == null || request.getStatus().equalsIgnoreCase(a.getStatus()))
                    .filter(a -> filterByDate(a.getAttendanceDate(), from, to))
                    .collect(Collectors.toList());
            data.setReportingPeriod(periodLabel(request));
        }

        List<String> columns = List.of("Date", "Student", "Admission No", "Class", "Stream", "Status", "Remarks");
        List<List<String>> rows = new ArrayList<>();
        for (Attendance a : records) {
            StudentProfile s = student(a.getStudentId());
            rows.add(List.of(a.getAttendanceDate().format(DATE_FMT), s == null ? "Unknown" : fullName(s),
                    s == null ? "-" : s.getAdmissionNumber(), nameOfClass(a.getClassId()),
                    nameOfStream(a.getStreamId()), a.getStatus(), a.getRemarks() == null ? "-" : a.getRemarks()));
        }
        data.setColumns(columns);
        data.setRows(rows);
        data.setSummaryStats(attendanceSummary(records));
    }

    private boolean filterByDate(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return false;
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private List<Attendance> filterByDate(List<Attendance> records, LocalDate from, LocalDate to) {
        return records.stream().filter(a -> filterByDate(a.getAttendanceDate(), from, to)).collect(Collectors.toList());
    }

    private List<String[]> attendanceSummary(List<Attendance> records) {
        long total = records.size();
        long present = records.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long absent = records.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long late = records.stream().filter(a -> "LATE".equalsIgnoreCase(a.getStatus())).count();
        double pct = total > 0 ? (present * 100.0 / total) : 0;
        return Arrays.asList(
                new String[] { "Total Records", String.valueOf(total) },
                new String[] { "Present", String.valueOf(present) },
                new String[] { "Absent", String.valueOf(absent) },
                new String[] { "Late", String.valueOf(late) },
                new String[] { "Attendance Rate", String.format("%.1f%%", pct) }
        );
    }

    /* ------------------------- results ------------------------- */
    private void buildResultsReport(ReportRenderData data, CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        Long examinationId = request.getExaminationId();
        List<Result> results;
        if (examinationId == null) {
            results = List.of();
        } else if ("RESULTS_BY_CLASS".equals(request.getReportType())) {
            results = resultRepository.findByExaminationIdAndStatus(examinationId, "PUBLISHED").stream()
                    .filter(r -> schoolId.equals(r.getSchoolId()))
                    .collect(Collectors.toList());
        } else {
            results = resultRepository.findByExaminationIdAndSchoolId(examinationId, schoolId,
                    PageRequest.of(0, 2000)).getContent();
        }
        results = filterResults(results, request);

        Map<Long, ExaminationSubject> esCache = new HashMap<>();
        Map<Long, StudentProfile> studentCache = new HashMap<>();
        List<String> columns = List.of("Student", "Admission No", "Class", "Stream", "Subject", "Marks", "Percentage", "Grade", "Status");
        List<List<String>> rows = new ArrayList<>();
        for (Result r : results) {
            ExaminationSubject es = esCache.computeIfAbsent(r.getExaminationSubjectId(), id -> examinationSubjectRepository.findById(id).orElse(null));
            StudentProfile s = studentCache.computeIfAbsent(r.getStudentId(), id -> studentRepository.findBySchoolIdAndId(schoolId, id).orElse(null));
            rows.add(List.of(
                    s == null ? "Unknown" : fullName(s),
                    s == null ? "-" : s.getAdmissionNumber(),
                    es == null ? "-" : nameOfClass(es.getClassId()),
                    es == null ? "-" : nameOfStream(es.getStreamId()),
                    es == null ? "-" : nameOfSubject(es.getSubjectId()),
                    r.getMarksObtained() == null ? "-" : r.getMarksObtained().toString(),
                    r.getPercentage() == null ? "-" : r.getPercentage().toString(),
                    r.getGrade() == null ? "-" : r.getGrade(),
                    r.getStatus() == null ? "-" : r.getStatus()));
        }
        data.setColumns(columns);
        data.setRows(rows);
        data.setSummaryStats(resultsSummary(results));
    }

    private List<Result> filterResults(List<Result> results, CreateReportJobRequest request) {
        return results.stream()
                .filter(r -> request.getClassId() == null || classOfResult(r).equals(request.getClassId()))
                .filter(r -> request.getStreamId() == null || streamOfResult(r).equals(request.getStreamId()))
                .filter(r -> request.getSubjectId() == null || subjectOfResult(r).equals(request.getSubjectId()))
                .filter(r -> request.getStudentId() == null || request.getStudentId().equals(r.getStudentId()))
                .filter(r -> request.getStatus() == null || request.getStatus().equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
    }

    private Long classOfResult(Result r) {
        try {
            ExaminationSubject es = examinationSubjectRepository.findById(r.getExaminationSubjectId()).orElse(null);
            return es == null ? null : es.getClassId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long streamOfResult(Result r) {
        try {
            ExaminationSubject es = examinationSubjectRepository.findById(r.getExaminationSubjectId()).orElse(null);
            return es == null ? null : es.getStreamId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long subjectOfResult(Result r) {
        try {
            ExaminationSubject es = examinationSubjectRepository.findById(r.getExaminationSubjectId()).orElse(null);
            return es == null ? null : es.getSubjectId();
        } catch (Exception e) {
            return null;
        }
    }

    private List<String[]> resultsSummary(List<Result> results) {
        long total = results.size();
        long published = results.stream().filter(r -> "PUBLISHED".equalsIgnoreCase(r.getStatus())).count();
        BigDecimal avg = results.stream().filter(r -> r.getPercentage() != null)
                .map(Result::getPercentage).reduce(BigDecimal.ZERO, BigDecimal::add);
        avg = total > 0 ? avg.divide(BigDecimal.valueOf(total), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        return Arrays.asList(
                new String[] { "Total Results", String.valueOf(total) },
                new String[] { "Published", String.valueOf(published) },
                new String[] { "Average Percentage", avg.toString() + "%" }
        );
    }

    /* ------------------------- teacher ------------------------- */
    private void buildTeacherReport(ReportRenderData data, CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        String type = request.getReportType();
        List<TeacherProfile> teachers;
        if ("TEACHER_DETAILS".equals(type) && request.getStudentId() != null) {
            teachers = new ArrayList<>();
            teacherProfileRepository.findBySchoolIdAndId(schoolId, request.getStudentId())
                    .ifPresent(teachers::add);
        } else {
            teachers = teacherProfileRepository.findBySchoolId(schoolId,
                    PageRequest.of(0, 1000)).getContent();
            teachers = teachers.stream()
                    .filter(t -> request.getDepartmentId() == null || teacherInDepartment(t.getId(), request.getDepartmentId()))
                    .filter(t -> request.getStatus() == null || request.getStatus().equalsIgnoreCase(t.getEmploymentStatus()))
                    .collect(Collectors.toList());
        }
        if ("TEACHERS_BY_DEPARTMENT".equals(type)) {
            teachers = teachers.stream()
                    .filter(t -> request.getDepartmentId() != null && teacherInDepartment(t.getId(), request.getDepartmentId()))
                    .collect(Collectors.toList());
        }

        List<String> columns = List.of("Employee No", "Full Name", "Gender", "DOB", "Employment Status", "Qualification", "Experience (Years)");
        List<List<String>> rows = new ArrayList<>();
        for (TeacherProfile t : teachers) {
            rows.add(List.of(
                    t.getEmployeeNumber() == null ? "-" : t.getEmployeeNumber(),
                    fullName(user(t.getUserId())),
                    t.getGender() == null ? "-" : t.getGender(),
                    t.getDateOfBirth() == null ? "-" : t.getDateOfBirth().format(DATE_FMT),
                    t.getEmploymentStatus() == null ? "-" : t.getEmploymentStatus(),
                    t.getQualification() == null ? "-" : t.getQualification(),
                    t.getExperienceYears() == null ? "-" : String.valueOf(t.getExperienceYears())));
        }
        data.setColumns(columns);
        data.setRows(rows);
        data.setSummaryStats(Arrays.asList(
                new String[] { "Total Teachers", String.valueOf(teachers.size()) },
                new String[] { "Active", String.valueOf(teachers.stream().filter(t -> "ACTIVE".equalsIgnoreCase(t.getEmploymentStatus())).count()) }));
    }

    /* ------------------------- finance ------------------------- */
    private void buildFinanceReport(ReportRenderData data, CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        String type = request.getReportType();
        LocalDate from = request.getDateFrom();
        LocalDate to = request.getDateTo();

        switch (type) {
            case "PAYMENTS_REPORT": {
                List<com.edumanage.fees.entity.Payment> payments = paymentRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, 2000)).getContent();
                payments = payments.stream()
                        .filter(p -> filterByDate(p.getPaymentDate(), from, to))
                        .filter(p -> request.getStudentId() == null || request.getStudentId().equals(p.getStudentId()))
                        .collect(Collectors.toList());
                List<String> columns = List.of("Date", "Student", "Admission No", "Amount", "Method", "Reference", "Status");
                List<List<String>> rows = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                for (Payment p : payments) {
                    StudentProfile s = student(p.getStudentId());
                    rows.add(List.of(p.getPaymentDate().format(DATE_FMT),
                            s == null ? "Unknown" : fullName(s),
                            s == null ? "-" : s.getAdmissionNumber(),
                            p.getAmount().toString(), p.getPaymentMethod(),
                            p.getReferenceNumber() == null ? "-" : p.getReferenceNumber(), p.getStatus()));
                    if ("COMPLETED".equalsIgnoreCase(p.getStatus())) {
                        total = total.add(p.getAmount());
                    }
                }
                data.setColumns(columns);
                data.setRows(rows);
                data.setSummaryStats(Arrays.asList(
                        new String[] { "Total Payments", String.valueOf(payments.size()) },
                        new String[] { "Collected Amount", total.toString() }));
                break;
            }
            case "OUTSTANDING_FEES_REPORT": {
                List<FeeAssignment> assignments = feeAssignmentRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, 2000)).getContent();
                assignments = assignments.stream()
                        .filter(f -> f.getBalance() != null && f.getBalance().compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.toList());
                List<String> columns = List.of("Student", "Admission No", "Amount Due", "Amount Paid", "Balance", "Due Date", "Status");
                List<List<String>> rows = new ArrayList<>();
                BigDecimal totalBalance = BigDecimal.ZERO;
                for (FeeAssignment f : assignments) {
                    StudentProfile s = student(f.getStudentId());
                    rows.add(List.of(
                            s == null ? "Unknown" : fullName(s),
                            s == null ? "-" : s.getAdmissionNumber(),
                            f.getAmountDue().toString(), f.getAmountPaid().toString(), f.getBalance().toString(),
                            f.getDueDate() == null ? "-" : f.getDueDate().format(DATE_FMT), f.getStatus()));
                    totalBalance = totalBalance.add(f.getBalance());
                }
                data.setColumns(columns);
                data.setRows(rows);
                data.setSummaryStats(Arrays.asList(
                        new String[] { "Students with Balance", String.valueOf(assignments.size()) },
                        new String[] { "Total Outstanding", totalBalance.toString() }));
                break;
            }
            case "FEE_COLLECTION_SUMMARY": {
                LocalDate fFrom = from == null ? LocalDate.MIN : from;
                LocalDate fTo = to == null ? LocalDate.MAX : to;
                BigDecimal billed = feeAssignmentRepository.sumAmountDueBetween(schoolId, fFrom, fTo);
                BigDecimal collected = feeAssignmentRepository.sumAmountPaidBetween(schoolId, fFrom, fTo);
                BigDecimal balance = feeAssignmentRepository.sumBalanceBetween(schoolId, fFrom, fTo);
                List<String> columns = List.of("Metric", "Amount");
                List<List<String>> rows = List.of(
                        List.of("Total Billed", billed == null ? "0.00" : billed.toString()),
                        List.of("Total Collected", collected == null ? "0.00" : collected.toString()),
                        List.of("Total Outstanding", balance == null ? "0.00" : balance.toString()));
                data.setColumns(columns);
                data.setRows(rows);
                data.setSummaryStats(Arrays.asList(
                        new String[] { "Billed", billed == null ? "0.00" : billed.toString() },
                        new String[] { "Collected", collected == null ? "0.00" : collected.toString() },
                        new String[] { "Outstanding", balance == null ? "0.00" : balance.toString() }));
                break;
            }
            case "PAYMENT_HISTORY": {
                List<Payment> payments = request.getStudentId() == null ? List.of()
                        : paymentRepository.findBySchoolIdAndStudentId(schoolId, request.getStudentId());
                payments = payments.stream().filter(p -> filterByDate(p.getPaymentDate(), from, to))
                        .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                        .collect(Collectors.toList());
                List<String> columns = List.of("Date", "Amount", "Method", "Reference", "Receipt", "Status");
                List<List<String>> rows = new ArrayList<>();
                for (Payment p : payments) {
                    rows.add(List.of(p.getPaymentDate().format(DATE_FMT), p.getAmount().toString(),
                            p.getPaymentMethod(), p.getReferenceNumber() == null ? "-" : p.getReferenceNumber(),
                            p.getReceiptNumber() == null ? "-" : p.getReceiptNumber(), p.getStatus()));
                }
                data.setColumns(columns);
                data.setRows(rows);
                break;
            }
            case "FINANCIAL_TRANSACTIONS_REPORT": {
                List<Payment> payments = paymentRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, 2000)).getContent();
                payments = payments.stream().filter(p -> filterByDate(p.getPaymentDate(), from, to)).collect(Collectors.toList());
                List<String> columns = List.of("Date", "Student", "Amount", "Method", "Reference", "Status");
                List<List<String>> rows = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                for (Payment p : payments) {
                    StudentProfile s = student(p.getStudentId());
                    rows.add(List.of(p.getPaymentDate().format(DATE_FMT),
                            s == null ? "Unknown" : fullName(s), p.getAmount().toString(),
                            p.getPaymentMethod(), p.getReferenceNumber() == null ? "-" : p.getReferenceNumber(), p.getStatus()));
                    total = total.add(p.getAmount());
                }
                data.setColumns(columns);
                data.setRows(rows);
                data.setSummaryStats(Arrays.asList(
                        new String[] { "Total Transactions", String.valueOf(payments.size()) },
                        new String[] { "Total Amount", total.toString() }));
                break;
            }
            default:
                throw new ForbiddenException("Unsupported finance report type: " + type);
        }
    }

    /* ------------------------- boarding ------------------------- */
    private void buildBoardingReport(ReportRenderData data, CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        String type = request.getReportType();
        List<BoardingStudent> boardings = boardingStudentRepository.findBySchoolId(schoolId,
                PageRequest.of(0, 1000)).getContent();
        boardings = boardings.stream()
                .filter(b -> request.getStatus() == null || request.getStatus().equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());

        List<String> columns = List.of("Student", "Admission No", "Dormitory", "Room", "Bed", "Allocated Date", "Status");
        List<List<String>> rows = new ArrayList<>();
        for (BoardingStudent b : boardings) {
            StudentProfile s = student(b.getStudentId());
            rows.add(List.of(
                    s == null ? "Unknown" : fullName(s),
                    s == null ? "-" : s.getAdmissionNumber(),
                    nameOfDormitory(b.getDormitoryId()), nameOfRoom(b.getRoomId()), nameOfBed(b.getBedId()),
                    b.getAllocatedDate() == null ? "-" : b.getAllocatedDate().format(DATE_FMT),
                    b.getStatus() == null ? "-" : b.getStatus()));
        }
        data.setColumns(columns);
        data.setRows(rows);
        data.setSummaryStats(Arrays.asList(
                new String[] { "Total Boarding Students", String.valueOf(boardings.size()) },
                new String[] { "Active", String.valueOf(boardings.stream().filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus())).count()) }));
    }

    /* ------------------------- communication ------------------------- */
    private void buildCommunicationReport(ReportRenderData data, CreateReportJobRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        String type = request.getReportType();
        LocalDate from = request.getDateFrom();
        LocalDate to = request.getDateTo();

        List<String> columns;
        List<List<String>> rows;
        List<String[]> stats;

        if ("SMS_HISTORY".equals(type) || "NOTIFICATION_HISTORY".equals(type)) {
            List<SmsMessage> messages = smsMessageRepository.findBySchoolId(schoolId,
                    PageRequest.of(0, 2000)).getContent();
            messages = messages.stream()
                    .filter(m -> filterByDateTime(m.getSentAt(), from, to))
                    .filter(m -> request.getStatus() == null || request.getStatus().equalsIgnoreCase(m.getStatus()))
                    .collect(Collectors.toList());
            columns = List.of("Date", "Recipient", "Status", "Message");
            rows = new ArrayList<>();
            for (SmsMessage m : messages) {
                rows.add(List.of(
                        m.getSentAt() == null ? "-" : m.getSentAt().format(DATE_TIME_FMT),
                        m.getRecipient(), m.getStatus() == null ? "-" : m.getStatus(),
                        truncate(m.getMessage(), 60)));
            }
            stats = Arrays.asList(new String[][] { new String[] { "Total Messages", String.valueOf(messages.size()) } });
        } else if ("COMMUNICATION_CAMPAIGN_REPORT".equals(type)) {
            List<com.edumanage.notifications.entity.CommunicationCampaign> campaigns = communicationCampaignRepository.findAll()
                    .stream().filter(c -> schoolId.equals(c.getSchoolId()))
                    .filter(c -> filterByDateTime(c.getCreatedAt(), from, to))
                    .collect(Collectors.toList());
            columns = List.of("Campaign", "Recipients", "Status", "Created");
            rows = new ArrayList<>();
            for (com.edumanage.notifications.entity.CommunicationCampaign c : campaigns) {
                rows.add(List.of(c.getTitle() == null ? "-" : c.getTitle(),
                        String.valueOf(c.getIntendedCount()),
                        c.getStatus() == null ? "-" : c.getStatus(),
                        c.getCreatedAt() == null ? "-" : c.getCreatedAt().format(DATE_TIME_FMT)));
            }
            stats = Arrays.asList(new String[][] { new String[] { "Total Campaigns", String.valueOf(campaigns.size()) } });
        } else {
            List<com.edumanage.notifications.entity.CommunicationDelivery> deliveries = communicationDeliveryRepository.findAll()
                    .stream().filter(d -> {
                        // delivery has no schoolId directly; join via campaign
                        try {
                            var campaign = communicationCampaignRepository.findById(d.getCampaignId()).orElse(null);
                            return campaign != null && schoolId.equals(campaign.getSchoolId());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .filter(d -> filterByDateTime(d.getSentAt(), from, to))
                    .collect(Collectors.toList());
            long sent = deliveries.stream().filter(d -> "SENT".equalsIgnoreCase(d.getStatus())).count();
            long delivered = deliveries.stream().filter(d -> "DELIVERED".equalsIgnoreCase(d.getStatus())).count();
            long failed = deliveries.stream().filter(d -> "FAILED".equalsIgnoreCase(d.getStatus())).count();
            columns = List.of("Delivery Status", "Count");
            rows = List.of(
                    List.of("Sent", String.valueOf(sent)),
                    List.of("Delivered", String.valueOf(delivered)),
                    List.of("Failed", String.valueOf(failed)));
            stats = Arrays.asList(new String[][] { new String[] { "Total Deliveries", String.valueOf(deliveries.size()) } });
        }
        data.setColumns(columns);
        data.setRows(rows);
        data.setSummaryStats(stats);
    }

    private boolean filterByDateTime(LocalDateTime dateTime, LocalDate from, LocalDate to) {
        if (dateTime == null) return false;
        if (from != null && dateTime.toLocalDate().isBefore(from)) return false;
        if (to != null && dateTime.toLocalDate().isAfter(to)) return false;
        return true;
    }

    /* ------------------------- helpers ------------------------- */
    private String fullName(StudentProfile s) {
        if (s == null) return "Unknown";
        String middle = s.getMiddleName() == null ? "" : " " + s.getMiddleName();
        return s.getFirstName() + middle + " " + s.getLastName();
    }

    private String fullName(User u) {
        if (u == null) return "Unknown";
        return (u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName());
    }

    private User user(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    private StudentProfile student(Long studentId) {
        if (studentId == null) return null;
        Long schoolId = TenantContext.requireTenantId();
        return studentRepository.findBySchoolIdAndId(schoolId, studentId).orElse(null);
    }

    private String nameOfClass(Long classId) {
        if (classId == null) return "-";
        Long schoolId = TenantContext.requireTenantId();
        return classRepository.findBySchoolIdAndId(schoolId, classId)
                .map(Class::getName).orElse("-");
    }

    private String nameOfStream(Long streamId) {
        if (streamId == null) return "-";
        Long schoolId = TenantContext.requireTenantId();
        return streamRepository.findBySchoolIdAndId(schoolId, streamId)
                .map(com.edumanage.streams.entity.Stream::getName).orElse("-");
    }

    private String nameOfDepartment(Long departmentId) {
        if (departmentId == null) return "-";
        Long schoolId = TenantContext.requireTenantId();
        return departmentRepository.findBySchoolIdAndId(schoolId, departmentId)
                .map(com.edumanage.departments.entity.Department::getName).orElse("-");
    }

    private String nameOfSubject(Long subjectId) {
        if (subjectId == null) return "-";
        Long schoolId = TenantContext.requireTenantId();
        return subjectRepository.findBySchoolIdAndId(schoolId, subjectId)
                .map(Subject::getName).orElse("-");
    }

    private String nameOfAcademicYear(Long yearId) {
        if (yearId == null) return "-";
        Long schoolId = TenantContext.requireTenantId();
        return academicYearRepository.findBySchoolIdAndId(schoolId, yearId)
                .map(AcademicYear::getName).orElse("-");
    }

    private String nameOfExamination(Long examinationId) {
        if (examinationId == null) return "-";
        Long schoolId = TenantContext.requireTenantId();
        return examinationRepository.findBySchoolIdAndId(schoolId, examinationId)
                .map(Examination::getName).orElse("-");
    }

    private String nameOfUser(Long userId) {
        if (userId == null) return "-";
        return userRepository.findById(userId)
                .map(u -> (u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName()))
                .orElse("-");
    }

    private StudentEnrollment enrollmentOf(Long studentId, Long academicYearId) {
        if (studentId == null) return null;
        Long schoolId = TenantContext.requireTenantId();
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudentIdAndSchoolId(studentId, schoolId);
        if (enrollments.isEmpty()) return null;
        if (academicYearId != null) {
            for (StudentEnrollment e : enrollments) {
                if (academicYearId.equals(e.getAcademicYearId())) return e;
            }
        }
        return enrollments.get(0);
    }

    private Long classIdOfStudent(Long studentId) {
        StudentEnrollment e = enrollmentOf(studentId, null);
        return e == null ? null : e.getClassId();
    }

    private Long streamIdOfStudent(Long studentId) {
        StudentEnrollment e = enrollmentOf(studentId, null);
        return e == null ? null : e.getStreamId();
    }

    private boolean teacherInDepartment(Long teacherId, Long departmentId) {
        if (teacherId == null || departmentId == null) return false;
        Long schoolId = TenantContext.requireTenantId();
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findByTeacherIdAndSchoolId(teacherId, schoolId);
        return assignments.stream().anyMatch(a -> {
            Subject sub = subjectRepository.findBySchoolIdAndId(schoolId, a.getSubjectId()).orElse(null);
            return sub != null && departmentId.equals(sub.getDepartmentId());
        });
    }

    private String nameOfDormitory(Long id) {
        if (id == null) return "-";
        return dormitoryRepository.findById(id).map(d -> d.getName() == null ? "-" : d.getName()).orElse("-");
    }

    private String nameOfRoom(Long id) {
        if (id == null) return "-";
        return roomRepository.findById(id).map(r -> r.getRoomNumber() == null ? "-" : r.getRoomNumber()).orElse("-");
    }

    private String nameOfBed(Long id) {
        if (id == null) return "-";
        return bedRepository.findById(id).map(b -> b.getBedNumber() == null ? "-" : b.getBedNumber()).orElse("-");
    }

    private String periodLabel(CreateReportJobRequest request) {
        if (request.getDateFrom() != null && request.getDateTo() != null) {
            return request.getDateFrom().format(DATE_FMT) + " to " + request.getDateTo().format(DATE_FMT);
        }
        if (request.getDateFrom() != null) {
            return "From " + request.getDateFrom().format(DATE_FMT);
        }
        if (request.getDateTo() != null) {
            return "To " + request.getDateTo().format(DATE_FMT);
        }
        return "-";
    }

    private String typeLabel(String type) {
        switch (type) {
            case "STUDENT_DIRECTORY": return "Student Directory";
            case "STUDENTS_BY_CLASS": return "Students by Class";
            case "STUDENTS_BY_STREAM": return "Students by Stream";
            case "STUDENT_ENROLLMENT_REPORT": return "Student Enrollment Report";
            case "STUDENT_ADMISSION_REPORT": return "Student Admission Report";
            case "STUDENT_STATUS_REPORT": return "Student Status Report";
            case "CLASS_REPORT": return "Class Report";
            case "STREAM_REPORT": return "Stream Report";
            case "SUBJECT_REPORT": return "Subject Report";
            case "TEACHING_ASSIGNMENTS_REPORT": return "Teaching Assignments Report";
            case "ACADEMIC_YEAR_REPORT": return "Academic Year Report";
            case "TIMETABLE_REPORT": return "Timetable Report";
            case "DEPARTMENT_REPORT": return "Department Report";
            case "DAILY_ATTENDANCE": return "Daily Attendance";
            case "ATTENDANCE_BY_DATE_RANGE": return "Attendance by Date Range";
            case "ATTENDANCE_BY_CLASS": return "Attendance by Class";
            case "ATTENDANCE_BY_STREAM": return "Attendance by Stream";
            case "INDIVIDUAL_STUDENT_ATTENDANCE": return "Individual Student Attendance";
            case "ATTENDANCE_SUMMARY": return "Attendance Summary";
            case "EXAMINATION_RESULTS": return "Examination Results";
            case "RESULTS_BY_CLASS": return "Results by Class";
            case "RESULTS_BY_STREAM": return "Results by Stream";
            case "RESULTS_BY_SUBJECT": return "Results by Subject";
            case "INDIVIDUAL_STUDENT_RESULTS": return "Individual Student Results";
            case "EXAMINATION_PERFORMANCE_SUMMARY": return "Examination Performance Summary";
            case "GRADE_DISTRIBUTION": return "Grade Distribution";
            case "SUBJECT_PERFORMANCE_REPORT": return "Subject Performance Report";
            case "TEACHER_DIRECTORY": return "Teacher Directory";
            case "TEACHER_DETAILS": return "Teacher Details";
            case "TEACHERS_BY_DEPARTMENT": return "Teachers by Department";
            case "TEACHING_ASSIGNMENTS": return "Teaching Assignments";
            case "PAYMENTS_REPORT": return "Payments Report";
            case "OUTSTANDING_FEES_REPORT": return "Outstanding Fees Report";
            case "FEE_COLLECTION_SUMMARY": return "Fee Collection Summary";
            case "PAYMENT_HISTORY": return "Payment History";
            case "FINANCIAL_TRANSACTIONS_REPORT": return "Financial Transactions Report";
            case "BOARDING_STUDENTS": return "Boarding Students";
            case "BOARDING_ALLOCATIONS": return "Boarding Allocations";
            case "BOARDING_OCCUPANCY": return "Boarding Occupancy";
            case "BOARDING_STATUS_REPORT": return "Boarding Status Report";
            case "SMS_HISTORY": return "SMS History";
            case "COMMUNICATION_CAMPAIGN_REPORT": return "Communication Campaign Report";
            case "NOTIFICATION_HISTORY": return "Notification History";
            case "DELIVERY_STATUS_SUMMARY": return "Delivery Status Summary";
            default: return type;
        }
    }

    private long studentCountByClass(Long classId) {
        Long schoolId = TenantContext.requireTenantId();
        return studentRepository.findBySchoolIdAndDeletedAtIsNull(schoolId,
                PageRequest.of(0, 1)).getContent().stream()
                .filter(s -> classId.equals(classIdOfStudent(s.getId()))).count();
    }

    private List<ReportOptionDto> toOptions(List<?> items) {
        List<ReportOptionDto> options = new ArrayList<>();
        for (Object item : items) {
            String id = null;
            String name = null;
            if (item instanceof AcademicYear ay) { id = String.valueOf(ay.getId()); name = ay.getName(); }
            else if (item instanceof Class c) { id = String.valueOf(c.getId()); name = c.getName(); }
            else if (item instanceof com.edumanage.streams.entity.Stream s) { id = String.valueOf(s.getId()); name = s.getName(); }
            else if (item instanceof com.edumanage.departments.entity.Department d) { id = String.valueOf(d.getId()); name = d.getName(); }
            else if (item instanceof Subject s) { id = String.valueOf(s.getId()); name = s.getName(); }
            else if (item instanceof Examination e) { id = String.valueOf(e.getId()); name = e.getName(); }
            else if (item instanceof StudentProfile s) { id = String.valueOf(s.getId()); name = s.getAdmissionNumber() + " - " + fullName(s); }
            if (id != null && name != null) {
                options.add(new ReportOptionDto(id, name));
            }
        }
        return options;
    }

    private List<ReportOptionDto> staticOptions(String... values) {
        List<ReportOptionDto> options = new ArrayList<>();
        for (String v : values) {
            options.add(new ReportOptionDto(v, v));
        }
        return options;
    }

    private String truncate(String text, int max) {
        if (text == null) return "-";
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String sanitize(String name) {
        if (name == null) return "report";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private StoredFile storePdf(Long schoolId, Long jobId, String reportName, byte[] pdf) throws IOException {
        Path schoolDir = storageRoot.resolve(schoolId.toString());
        Files.createDirectories(schoolDir);
        Path file = schoolDir.resolve(jobId + ".pdf");
        Files.write(file, pdf);
        String fileName = sanitize(reportName) + ".pdf";
        String fileUrl = "/school/reports/" + jobId + "/download";
        return new StoredFile(fileUrl, fileName, (long) pdf.length);
    }

    private void updateStatus(Long jobId, String status, String fileUrl, String fileName, Long fileSize, String errorMessage) {
        ReportJob job = reportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Report job not found"));
        job.setStatus(status);
        job.setFileUrl(fileUrl);
        job.setFileName(fileName);
        job.setFileSize(fileSize);
        job.setErrorMessage(errorMessage);
        if ("PROCESSING".equals(status)) {
            job.setStartedAt(LocalDateTime.now());
        } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            job.setCompletedAt(LocalDateTime.now());
        }
        reportJobRepository.save(job);
    }

    private com.edumanage.reports.dto.ReportJobDto toDto(ReportJob entity) {
        com.edumanage.reports.dto.ReportJobDto dto = new com.edumanage.reports.dto.ReportJobDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setRequestedBy(entity.getRequestedBy());
        dto.setReportType(entity.getReportType());
        dto.setReportName(entity.getReportName());
        dto.setParameters(entity.getParameters());
        dto.setStatus(entity.getStatus());
        dto.setFileUrl(entity.getFileUrl());
        dto.setFileName(entity.getFileName());
        dto.setFileSize(entity.getFileSize());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private record StoredFile(String fileUrl, String fileName, Long fileSize) {
    }

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "STUDENT_DIRECTORY", "STUDENTS_BY_CLASS", "STUDENTS_BY_STREAM", "STUDENT_ENROLLMENT_REPORT",
            "STUDENT_ADMISSION_REPORT", "STUDENT_STATUS_REPORT",
            "CLASS_REPORT", "STREAM_REPORT", "SUBJECT_REPORT", "TEACHING_ASSIGNMENTS_REPORT",
            "ACADEMIC_YEAR_REPORT", "TIMETABLE_REPORT", "DEPARTMENT_REPORT",
            "DAILY_ATTENDANCE", "ATTENDANCE_BY_DATE_RANGE", "ATTENDANCE_BY_CLASS", "ATTENDANCE_BY_STREAM",
            "INDIVIDUAL_STUDENT_ATTENDANCE", "ATTENDANCE_SUMMARY",
            "EXAMINATION_RESULTS", "RESULTS_BY_CLASS", "RESULTS_BY_STREAM", "RESULTS_BY_SUBJECT",
            "INDIVIDUAL_STUDENT_RESULTS", "EXAMINATION_PERFORMANCE_SUMMARY", "GRADE_DISTRIBUTION", "SUBJECT_PERFORMANCE_REPORT",
            "TEACHER_DIRECTORY", "TEACHER_DETAILS", "TEACHERS_BY_DEPARTMENT", "TEACHING_ASSIGNMENTS",
            "PAYMENTS_REPORT", "OUTSTANDING_FEES_REPORT", "FEE_COLLECTION_SUMMARY", "PAYMENT_HISTORY", "FINANCIAL_TRANSACTIONS_REPORT",
            "BOARDING_STUDENTS", "BOARDING_ALLOCATIONS", "BOARDING_OCCUPANCY", "BOARDING_STATUS_REPORT",
            "SMS_HISTORY", "COMMUNICATION_CAMPAIGN_REPORT", "NOTIFICATION_HISTORY", "DELIVERY_STATUS_SUMMARY"
    );

    private static final Set<String> WIDE_TYPES = Set.of(
            "RESULTS_BY_CLASS", "RESULTS_BY_STREAM", "FINANCIAL_TRANSACTIONS_REPORT", "ATTENDANCE_BY_DATE_RANGE"
    );

    /* ---------- filter definitions ---------- */
    private List<ReportTypeInfoDto> buildReportTypes() {
        List<ReportTypeInfoDto> types = new ArrayList<>();
        types.add(reportType("STUDENT_DIRECTORY", "Student Directory", "Student Reports", List.of(
                field("academicYear", "Academic Year", "select", false, "/school/reports/options/academicYears", null, null, null),
                field("class", "Class", "select", false, "/school/reports/options/classes", null, null, null),
                field("stream", "Stream", "select", false, "/school/reports/options/streams", "class", null, null),
                field("gender", "Gender", "select", false, null, null, staticOptions("MALE", "FEMALE", "OTHER"), null),
                field("status", "Student Status", "select", false, "/school/reports/options/statuses", null, null, null),
                field("student", "Specific Student", "select", false, "/school/reports/options/students", "class", null, null),
                field("includeFields", "Information to Include", "multiselect", false, null, null, null,
                    List.of(new ReportOptionDto("admissionNumber", "Admission Number"),
                            new ReportOptionDto("fullName", "Full Name"),
                            new ReportOptionDto("gender", "Gender"),
                            new ReportOptionDto("dateOfBirth", "Date of Birth"),
                            new ReportOptionDto("class", "Class"),
                            new ReportOptionDto("stream", "Stream"),
                            new ReportOptionDto("parentGuardian", "Parent/Guardian"),
                            new ReportOptionDto("contact", "Contact"),
                            new ReportOptionDto("enrollmentStatus", "Enrollment Status")))
        )));
        types.add(reportType("ATTENDANCE_BY_DATE_RANGE", "Attendance Report", "Attendance Reports", List.of(
                field("academicYear", "Academic Year", "select", false, "/school/reports/options/academicYears", null, null, null),
                field("dateFrom", "Date From", "date", false, null, null, null, null),
                field("dateTo", "Date To", "date", false, null, null, null, null),
                field("class", "Class", "select", false, "/school/reports/options/classes", null, null, null),
                field("stream", "Stream", "select", false, "/school/reports/options/streams", "class", null, null),
                field("status", "Attendance Status", "select", false, "/school/reports/options/attendanceStatuses", null, null, null)
        )));
        types.add(reportType("EXAMINATION_RESULTS", "Examination Results", "Examination Reports", List.of(
                field("academicYear", "Academic Year", "select", false, "/school/reports/options/academicYears", null, null, null),
                field("examination", "Examination", "select", false, "/school/reports/options/examinations", "academicYear", null, null),
                field("class", "Class", "select", false, "/school/reports/options/classes", null, null, null),
                field("stream", "Stream", "select", false, "/school/reports/options/streams", "class", null, null),
                field("subject", "Subject", "select", false, "/school/reports/options/subjects", "department", null, null),
                field("status", "Result Status", "select", false, "/school/reports/options/statuses", null, null, null)
        )));
        types.add(reportType("PAYMENTS_REPORT", "Payments Report", "Finance Reports", List.of(
                field("dateFrom", "Date From", "date", false, null, null, null, null),
                field("dateTo", "Date To", "date", false, null, null, null, null),
                field("student", "Student", "select", false, "/school/reports/options/students", null, null, null),
                field("status", "Payment Status", "select", false, "/school/reports/options/feeStatuses", null, null, null)
        )));
        types.add(reportType("TEACHER_DIRECTORY", "Teacher Directory", "Teacher Reports", List.of(
                field("department", "Department", "select", false, "/school/reports/options/departments", null, null, null),
                field("status", "Employment Status", "select", false, "/school/reports/options/teacherStatuses", null, null, null)
        )));
        types.add(reportType("BOARDING_STUDENTS", "Boarding Students", "Boarding Reports", List.of(
                field("status", "Boarding Status", "select", false, "/school/reports/options/boardingStatuses", null, null, null)
        )));
        types.add(reportType("SMS_HISTORY", "SMS History", "Communication Reports", List.of(
                field("dateFrom", "Date From", "date", false, null, null, null, null),
                field("dateTo", "Date To", "date", false, null, null, null, null),
                field("status", "Delivery Status", "select", false, "/school/reports/options/smsStatuses", null, null, null)
        )));
        return types;
    }

    private ReportTypeInfoDto reportType(String id, String label, String category, List<ReportFilterFieldDto> fields) {
        ReportTypeInfoDto t = new ReportTypeInfoDto();
        t.setId(id);
        t.setLabel(label);
        t.setCategory(category);
        t.setFields(fields);
        return t;
    }

    private ReportFilterFieldDto field(String name, String label, String type, boolean required,
                                       String endpoint, String dependsOn, List<ReportOptionDto> options,
                                       List<ReportOptionDto> multiOptions) {
        ReportFilterFieldDto f = new ReportFilterFieldDto();
        f.setName(name);
        f.setLabel(label);
        f.setType(type);
        f.setRequired(required);
        f.setOptionsEndpoint(endpoint);
        f.setDependsOn(dependsOn);
        f.setOptions(options == null ? multiOptions : options);
        return f;
    }
}
