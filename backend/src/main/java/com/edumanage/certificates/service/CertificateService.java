package com.edumanage.certificates.service;

import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.branding.repository.SchoolBrandingRepository;
import com.edumanage.certificates.dto.CertificateDto;
import com.edumanage.certificates.dto.CertificateRequest;
import com.edumanage.certificates.dto.CertificateVerificationDto;
import com.edumanage.certificates.dto.EligibleStudentDto;
import com.edumanage.certificates.entity.StudentCertificate;
import com.edumanage.certificates.repository.StudentCertificateRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.CurrentUserService;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class CertificateService {
    private static final String ACADEMIC_EXCELLENCE = "ACADEMIC_EXCELLENCE";

    private final StudentCertificateRepository certificateRepository;
    private final StudentProfileRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final SubjectRepository subjectRepository;
    private final ResultRepository resultRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolBrandingRepository brandingRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final CertificatePdfService pdfService;
    private final SecureRandom secureRandom = new SecureRandom();

    public CertificateService(StudentCertificateRepository certificateRepository,
                              StudentProfileRepository studentRepository,
                              StudentEnrollmentRepository enrollmentRepository,
                              AcademicYearRepository academicYearRepository,
                              ClassRepository classRepository,
                              StreamRepository streamRepository,
                              SubjectRepository subjectRepository,
                              ResultRepository resultRepository,
                              SchoolRepository schoolRepository,
                              SchoolBrandingRepository brandingRepository,
                              CurrentUserService currentUserService,
                              AuditLogService auditLogService,
                              CertificatePdfService pdfService) {
        this.certificateRepository = certificateRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.subjectRepository = subjectRepository;
        this.resultRepository = resultRepository;
        this.schoolRepository = schoolRepository;
        this.brandingRepository = brandingRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.pdfService = pdfService;
    }

    @Transactional(readOnly = true)
    public List<EligibleStudentDto> eligible(Long yearId, Long classId, Long streamId, Long subjectId) {
        Long schoolId = requireManagerSchool();
        validateScope(schoolId, yearId, classId, streamId, subjectId);
        return topResults(schoolId, yearId, classId, streamId, subjectId);
    }

    @Transactional(readOnly = true)
    public List<EligibleStudentDto> studentsInScope(Long yearId, Long classId, Long streamId) {
        Long schoolId = requireManagerSchool();
        validateScope(schoolId, yearId, classId, streamId, null);
        return enrollmentRepository.findBySchoolIdAndAcademicYearId(schoolId, yearId).stream()
                .filter(enrollment -> !enrollment.isDeleted() && classId.equals(enrollment.getClassId())
                        && java.util.Objects.equals(streamId, enrollment.getStreamId())
                        && isEligibleEnrollmentStatus(enrollment.getStatus()))
                .map(enrollment -> studentRepository.findById(enrollment.getStudentId())
                        .filter(student -> !student.isDeleted() && schoolId.equals(student.getSchoolId()))
                        .map(student -> new EligibleStudentDto(student.getId(),
                                fullName(student),
                                student.getAdmissionNumber(), null, null, null, null, null))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public CertificateDto generate(CertificateRequest request) {
        Long schoolId = requireManagerSchool();
        validateScope(schoolId, request.academicYearId(), request.classId(), request.streamId(), request.subjectId());
        StudentProfile student = studentRepository.findById(request.studentId())
                .filter(profile -> !profile.isDeleted() && schoolId.equals(profile.getSchoolId()))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found in this school"));
        validateEnrollment(student.getId(), schoolId, request.academicYearId(), request.classId(), request.streamId());

        String awardType = request.awardType().trim().toUpperCase(Locale.ROOT);
        Long resultId = null;
        if (ACADEMIC_EXCELLENCE.equals(awardType)) {
            if (request.subjectId() == null) throw new BadRequestException("A subject is required for Academic Excellence");
            EligibleStudentDto winner = topResults(schoolId, request.academicYearId(), request.classId(),
                    request.streamId(), request.subjectId()).stream()
                    .filter(candidate -> candidate.studentId().equals(student.getId()))
                    .findFirst().orElseThrow(() -> new BadRequestException("Student is not among the published top results"));
            resultId = winner.resultId();
        } else if (!List.of("PARTICIPATION", "ACHIEVEMENT", "RECOGNITION").contains(awardType)) {
            throw new BadRequestException("Unsupported certificate award type");
        }
        if (!ACADEMIC_EXCELLENCE.equals(awardType) && (request.description() == null || request.description().isBlank())) {
            throw new BadRequestException("Evidence or an achievement description is required for this award");
        }

        boolean duplicate = certificateRepository.existsActiveInScope(schoolId, student.getId(),
                request.academicYearId(), request.classId(), request.streamId(), request.subjectId(), awardType);
        if (duplicate) throw new ConflictException("An active certificate already exists for this award scope");

        StudentCertificate certificate = new StudentCertificate();
        certificate.setSchoolId(schoolId);
        certificate.setStudentId(student.getId());
        certificate.setAcademicYearId(request.academicYearId());
        certificate.setClassId(request.classId());
        certificate.setStreamId(request.streamId());
        certificate.setSubjectId(request.subjectId());
        certificate.setResultId(resultId);
        certificate.setAwardType(awardType);
        certificate.setTitle(ACADEMIC_EXCELLENCE.equals(awardType) ? "Certificate of Academic Excellence" : request.title().trim());
        certificate.setDescription(request.description());
        certificate.setCertificateNumber("EDU-" + token(10).toUpperCase(Locale.ROOT));
        certificate.setVerificationCode(token(16).toUpperCase(Locale.ROOT));
        certificate.setVersion(certificateRepository.nextVersion(schoolId, student.getId(),
                request.academicYearId(), request.classId(), request.streamId(), request.subjectId(), awardType));
        StudentCertificate saved;
        try {
            saved = certificateRepository.saveAndFlush(certificate);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A conflicting certificate already exists for this award scope");
        }
        auditLogService.logForSchool("CERTIFICATE_GENERATE", "StudentCertificate", saved.getId(),
                "Generated " + awardType + " certificate for student " + student.getId(), null, null, null, schoolId);
        return map(saved);
    }

    @Transactional
    public CertificateDto release(Long id) {
        Long schoolId = requireManagerSchool();
        StudentCertificate certificate = findCertificate(id, schoolId);
        if (!"GENERATED".equals(certificate.getStatus())) throw new BadRequestException("Only generated certificates can be released");
        if (ACADEMIC_EXCELLENCE.equals(certificate.getAwardType())) {
            boolean stillEligible = topResults(schoolId, certificate.getAcademicYearId(), certificate.getClassId(),
                    certificate.getStreamId(), certificate.getSubjectId()).stream()
                    .anyMatch(candidate -> candidate.studentId().equals(certificate.getStudentId())
                            && candidate.resultId().equals(certificate.getResultId()));
            if (!stillEligible) throw new BadRequestException("Published results changed; regenerate the certificate after reviewing eligibility");
        }
        User user = currentUserService.requireCurrentUser();
        certificate.setIssuedBy(user.getId());
        certificate.setIssuedAt(LocalDateTime.now());
        certificate.setStatus("RELEASED");
        auditLogService.logForSchool("CERTIFICATE_RELEASE", "StudentCertificate", certificate.getId(),
                "Released certificate " + certificate.getCertificateNumber(), null, null, null, schoolId);
        return map(certificateRepository.save(certificate));
    }

    @Transactional
    public CertificateDto revoke(Long id, String reason) {
        Long schoolId = requireManagerSchool();
        StudentCertificate certificate = findCertificate(id, schoolId);
        if (!"RELEASED".equals(certificate.getStatus())) throw new BadRequestException("Only released certificates can be revoked");
        if (reason == null || reason.isBlank()) throw new BadRequestException("A revocation reason is required");
        User user = currentUserService.requireCurrentUser();
        certificate.setStatus("REVOKED");
        certificate.setRevokedBy(user.getId());
        certificate.setRevokedAt(LocalDateTime.now());
        certificate.setRevocationReason(reason.trim());
        auditLogService.logForSchool("CERTIFICATE_REVOKE", "StudentCertificate", certificate.getId(),
                "Revoked certificate " + certificate.getCertificateNumber() + ": " + reason.trim(),
                null, null, null, schoolId);
        return map(certificateRepository.save(certificate));
    }

    @Transactional(readOnly = true)
    public List<CertificateDto> listForManager() {
        Long schoolId = requireManagerSchool();
        return certificateRepository.findBySchoolIdAndDeletedAtIsNullOrderByCreatedAtDesc(schoolId)
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<CertificateDto> listForCurrentStudent() {
        User user = currentUserService.requireCurrentUser();
        if (!currentUserService.hasRole(user, "STUDENT")) throw new ForbiddenException("Student access required");
        Long schoolId = TenantContext.requireTenantId();
        StudentProfile student = studentRepository.findByUserId(user.getId())
                .filter(profile -> !profile.isDeleted() && schoolId.equals(profile.getSchoolId()))
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        return certificateRepository.findByStudentIdAndSchoolIdAndDeletedAtIsNullOrderByCreatedAtDesc(student.getId(), schoolId)
                .stream().filter(certificate -> "RELEASED".equals(certificate.getStatus()) || "REVOKED".equals(certificate.getStatus()))
                .map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public byte[] document(Long id) {
        Long schoolId = TenantContext.requireTenantId();
        StudentCertificate certificate = findCertificate(id, schoolId);
        User user = currentUserService.requireCurrentUser();
        if (currentUserService.hasRole(user, "MANAGER")) {
            if (!"RELEASED".equals(certificate.getStatus())) throw new ForbiddenException("Only released certificates can be downloaded");
        } else if (currentUserService.hasRole(user, "STUDENT")) {
            StudentProfile student = studentRepository.findByUserId(user.getId())
                    .filter(profile -> !profile.isDeleted() && schoolId.equals(profile.getSchoolId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
            if (!student.getId().equals(certificate.getStudentId()) || !"RELEASED".equals(certificate.getStatus())) {
                throw new ForbiddenException("Certificate is unavailable");
            }
        } else {
            throw new ForbiddenException("Certificate access denied");
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        var branding = brandingRepository.findBySchoolId(schoolId).orElse(null);
        return pdfService.render(school, branding, map(certificate));
    }

    @Transactional(readOnly = true)
    public CertificateVerificationDto verify(String verificationCode) {
        StudentCertificate certificate = certificateRepository
                .findByVerificationCodeAndDeletedAtIsNull(verificationCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Certificate verification code not found"));
        if (!"RELEASED".equals(certificate.getStatus()) && !"REVOKED".equals(certificate.getStatus())) {
            throw new ResourceNotFoundException("Certificate verification code not found");
        }
        StudentProfile student = studentRepository.findById(certificate.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        School school = schoolRepository.findById(certificate.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        var year = academicYearRepository.findById(certificate.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        return new CertificateVerificationDto("RELEASED".equals(certificate.getStatus()), certificate.getStatus(),
                certificate.getCertificateNumber(), school.getName(), fullName(student), certificate.getTitle(),
                year.getName(), certificate.getIssuedAt());
    }

    private Long requireManagerSchool() {
        User user = currentUserService.requireCurrentUser();
        if (!currentUserService.hasRole(user, "MANAGER")) throw new ForbiddenException("School Manager access required");
        Long schoolId = TenantContext.requireTenantId();
        if (user.getSchoolId() != null && !schoolId.equals(user.getSchoolId())) throw new ForbiddenException("Access denied");
        return schoolId;
    }

    private void validateScope(Long schoolId, Long yearId, Long classId, Long streamId, Long subjectId) {
        academicYearRepository.findById(yearId).filter(year -> !year.isDeleted() && schoolId.equals(year.getSchoolId()))
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found in this school"));
        classRepository.findById(classId).filter(clazz -> !clazz.isDeleted() && schoolId.equals(clazz.getSchoolId()))
                .orElseThrow(() -> new ResourceNotFoundException("Class not found in this school"));
        if (streamId != null) {
            streamRepository.findById(streamId)
                    .filter(stream -> !stream.isDeleted() && schoolId.equals(stream.getSchoolId()) && classId.equals(stream.getClassId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Stream does not belong to the selected class"));
        }
        if (subjectId != null) {
            subjectRepository.findById(subjectId).filter(subject -> !subject.isDeleted() && schoolId.equals(subject.getSchoolId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found in this school"));
        }
    }

    private void validateEnrollment(Long studentId, Long schoolId, Long yearId, Long classId, Long streamId) {
        if (!isEnrolled(studentId, schoolId, yearId, classId, streamId)) {
            throw new BadRequestException("Student is not enrolled in the selected academic scope");
        }
    }

    private List<EligibleStudentDto> topResults(Long schoolId, Long yearId, Long classId, Long streamId, Long subjectId) {
        List<Result> results = resultRepository.findPublishedForCertificateScope(schoolId, yearId, classId, streamId, subjectId);
        if (results.isEmpty()) return List.of();
        List<Result> eligibleResults = results.stream()
                .filter(result -> isEnrolled(result.getStudentId(), schoolId, yearId, classId, streamId))
                .toList();
        if (eligibleResults.isEmpty()) return List.of();
        var schemeIds = eligibleResults.stream()
                .map(result -> result.getExaminationSubject().getGradingSchemeId()).distinct().toList();
        if (schemeIds.size() > 1) throw new BadRequestException("Published results use incompatible grading schemes; narrow the award scope");
        var candidates = eligibleResults.stream().map(result -> {
            StudentProfile student = studentRepository.findById(result.getStudentId())
                    .filter(profile -> !profile.isDeleted() && schoolId.equals(profile.getSchoolId()))
                    .orElse(null);
            if (student == null) return null;
            return new EligibleStudentDto(student.getId(),
                    fullName(student),
                    student.getAdmissionNumber(), result.getId(), result.getMarksObtained(), result.getPercentage(),
                    result.getGrade(), result.getExaminationSubject().getGradingSchemeId());
        }).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(EligibleStudentDto::studentId,
                        candidate -> candidate,
                (left, right) -> left.percentage().compareTo(right.percentage()) > 0
                        || (left.percentage().compareTo(right.percentage()) == 0 && left.resultId() >= right.resultId())
                        ? left : right));
        if (candidates.isEmpty()) return List.of();
        var max = candidates.values().stream().map(EligibleStudentDto::percentage).max(Comparator.naturalOrder()).orElseThrow();
        return candidates.values().stream().filter(candidate -> candidate.percentage().compareTo(max) == 0)
                .sorted(Comparator.comparing(EligibleStudentDto::studentName))
                .toList();
    }

    private boolean isEnrolled(Long studentId, Long schoolId, Long yearId, Long classId, Long streamId) {
        return enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId)
                .filter(enrollment -> !enrollment.isDeleted() && schoolId.equals(enrollment.getSchoolId())
                        && classId.equals(enrollment.getClassId())
                        && java.util.Objects.equals(streamId, enrollment.getStreamId())
                        && isEligibleEnrollmentStatus(enrollment.getStatus())).isPresent();
    }

    private boolean isEligibleEnrollmentStatus(String status) {
        return "ENROLLED".equals(status) || "COMPLETED".equals(status);
    }

    private StudentCertificate findCertificate(Long id, Long schoolId) {
        return certificateRepository.findByIdAndSchoolIdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));
    }

    private CertificateDto map(StudentCertificate certificate) {
        StudentProfile student = studentRepository.findById(certificate.getStudentId()).orElseThrow();
        var year = academicYearRepository.findById(certificate.getAcademicYearId()).orElseThrow();
        var clazz = classRepository.findById(certificate.getClassId()).orElseThrow();
        String streamName = certificate.getStreamId() == null ? null : streamRepository.findById(certificate.getStreamId())
                .map(stream -> stream.getName()).orElse(null);
        String subjectName = certificate.getSubjectId() == null ? null : subjectRepository.findById(certificate.getSubjectId())
                .map(subject -> subject.getName()).orElse(null);
        Result result = certificate.getResultId() == null ? null : resultRepository.findById(certificate.getResultId()).orElse(null);
        return new CertificateDto(certificate.getId(), student.getId(),
                fullName(student),
                student.getAdmissionNumber(), year.getId(), year.getName(), clazz.getId(), clazz.getName(),
                certificate.getStreamId(), streamName, certificate.getSubjectId(), subjectName,
                certificate.getResultId(), result == null ? null : result.getPercentage(),
                certificate.getAwardType(), certificate.getTitle(), certificate.getDescription(),
                certificate.getCertificateNumber(), certificate.getVerificationCode(), certificate.getStatus(),
                certificate.getVersion(), certificate.getIssuedAt(), certificate.getRevokedAt(), certificate.getRevocationReason());
    }

    private String token(int bytes) {
        byte[] value = new byte[bytes];
        secureRandom.nextBytes(value);
        return HexFormat.of().formatHex(value);
    }

    private String fullName(StudentProfile student) {
        return java.util.stream.Stream.of(student.getFirstName(), student.getMiddleName(), student.getLastName())
                .filter(name -> name != null && !name.isBlank()).map(String::trim).collect(java.util.stream.Collectors.joining(" "));
    }
}
