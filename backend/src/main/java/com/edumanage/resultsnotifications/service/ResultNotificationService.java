package com.edumanage.resultsnotifications.service;

import com.edumanage.audit.service.AuditLogService;
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
import com.edumanage.resultsnotifications.dto.ParentGuardianPreview;
import com.edumanage.resultsnotifications.dto.ResultNotificationDeliveryDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationPreview;
import com.edumanage.resultsnotifications.dto.ResultNotificationRequest;
import com.edumanage.resultsnotifications.dto.ResultNotificationSummary;
import com.edumanage.resultsnotifications.dto.StudentResultPreview;
import com.edumanage.resultsnotifications.entity.ResultNotification;
import com.edumanage.resultsnotifications.entity.ResultNotificationDelivery;
import com.edumanage.resultsnotifications.repository.ResultNotificationDeliveryRepository;
import com.edumanage.resultsnotifications.repository.ResultNotificationRepository;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.sms.dto.CreateSmsMessageRequest;
import com.edumanage.sms.dto.SmsMessageDto;
import com.edumanage.sms.entity.SmsMessage;
import com.edumanage.sms.repository.SmsMessageRepository;
import com.edumanage.sms.service.SmsMessageService;
import com.edumanage.streams.dto.StreamDto;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.studentparents.entity.StudentParent;
import com.edumanage.studentparents.repository.StudentParentRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultNotificationService {

    private static final List<String> PUBLISHABLE_STATUSES = List.of("PUBLISHED", "APPROVED");
    private static final int MAX_RECIPIENTS = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Permanent failures that must not be retried indefinitely.
     */
    private static final List<String> PERMANENT_FAILURE_CODES = List.of(
            "SMS_NOT_CONFIGURED",
            "SMS_PROVIDER_AUTH_FAILED",
            "SMS_PROVIDER_ACCOUNT_DISABLED",
            "SMS_PROVIDER_REJECTED_RECIPIENT",
            "SMS_PROVIDER_REJECTED",
            "SMS_PROVIDER_INVALID_RESPONSE",
            "INVALID_PHONE",
            "RECIPIENT_NO_LONGER_AVAILABLE"
    );

    private static final Logger log = LoggerFactory.getLogger(ResultNotificationService.class);

    private final ResultNotificationRepository notificationRepository;
    private final ResultNotificationDeliveryRepository deliveryRepository;
    private final ExaminationRepository examinationRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final ResultRepository resultRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final SchoolRepository schoolRepository;
    private final SmsMessageService smsMessageService;
    private final SmsMessageRepository smsMessageRepository;
    private final ResultNotificationTemplateService resultNotificationTemplateService;
    private final DarsmsProviderService darsmsProviderService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public List<ExaminationDto> getExaminations(Long schoolId) {
        return examinationRepository.findBySchoolId(schoolId,
                org.springframework.data.domain.PageRequest.of(0, 500)).getContent().stream()
                .map(this::mapExaminationToDto)
                .toList();
    }

    public List<com.edumanage.classes.entity.Class> getClasses(Long schoolId) {
        return classRepository.findBySchoolId(schoolId,
                org.springframework.data.domain.PageRequest.of(0, 500)).getContent();
    }

    public List<StreamDto> getStreams(Long schoolId, Long classId) {
        return streamRepository.findByClassIdAndSchoolId(classId, schoolId).stream()
                .map(this::mapStreamToDto)
                .toList();
    }

    private ExaminationDto mapExaminationToDto(Examination examination) {
        ExaminationDto dto = new ExaminationDto();
        dto.setId(examination.getId());
        dto.setUuid(examination.getUuid() != null ? examination.getUuid().toString() : null);
        dto.setSchoolId(examination.getSchoolId());
        dto.setAcademicYearId(examination.getAcademicYearId());
        dto.setAcademicYearName(null);
        dto.setName(examination.getName());
        dto.setTypeId(examination.getTypeId());
        dto.setTypeName(null);
        dto.setStartDate(examination.getStartDate());
        dto.setEndDate(examination.getEndDate());
        dto.setStatus(examination.getStatus());
        dto.setDescription(examination.getDescription());
        dto.setCreatedAt(examination.getCreatedAt());
        dto.setUpdatedAt(examination.getUpdatedAt());
        return dto;
    }

    private StreamDto mapStreamToDto(com.edumanage.streams.entity.Stream stream) {
        StreamDto dto = new StreamDto();
        dto.setId(stream.getId());
        dto.setUuid(stream.getUuid() != null ? stream.getUuid().toString() : null);
        dto.setSchoolId(stream.getSchoolId());
        dto.setClassId(stream.getClassId());
        dto.setClassName(null);
        dto.setName(stream.getName());
        dto.setCode(stream.getCode());
        dto.setCapacity(stream.getCapacity());
        dto.setStatus(stream.getStatus());
        dto.setCreatedAt(stream.getCreatedAt());
        dto.setUpdatedAt(stream.getUpdatedAt());
        return dto;
    }

    public void validateScope(Long schoolId, Long examinationId, Long classId, Long streamId) {
        Examination examination = examinationRepository.findBySchoolIdAndId(schoolId, examinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
        if (!examination.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Examination does not belong to this school");
        }

        com.edumanage.classes.entity.Class clazz = classRepository.findBySchoolIdAndId(schoolId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!clazz.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Class does not belong to this school");
        }

        if (streamId != null) {
            com.edumanage.streams.entity.Stream stream = streamRepository.findBySchoolIdAndId(schoolId, streamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (!stream.getSchoolId().equals(schoolId)) {
                throw new ForbiddenException("Stream does not belong to this school");
            }
            if (!stream.getClassId().equals(classId)) {
                throw new BadRequestException("Stream does not belong to the selected class");
            }
        }

        boolean hasSubjects = examinationSubjectRepository.findByExaminationId(examinationId,
                org.springframework.data.domain.PageRequest.of(0, 1000)).getContent().stream()
                .anyMatch(es -> es.getClassId().equals(classId)
                        && (streamId == null ? es.getStreamId() == null : es.getStreamId().equals(streamId)));
        if (!hasSubjects) {
            throw new BadRequestException("No examination subjects found for the selected examination, class and stream");
        }
    }

    @Transactional(readOnly = true)
    public ResultNotificationPreview preview(Long schoolId, Long examinationId, Long classId,
                                             Long streamId, String recipientType) {
        validateScope(schoolId, examinationId, classId, streamId);

        Examination examination = examinationRepository.findBySchoolIdAndId(schoolId, examinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
        com.edumanage.classes.entity.Class clazz = classRepository.findBySchoolIdAndId(schoolId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        com.edumanage.streams.entity.Stream stream = null;
        String streamName = null;
        if (streamId != null) {
            stream = streamRepository.findBySchoolIdAndId(schoolId, streamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            streamName = stream.getName();
        }

        String schoolName = schoolRepository.findById(schoolId)
                .map(s -> s.getName() != null ? s.getName() : "School")
                .orElse("School");

        List<Result> results = resultRepository.findByExaminationAndClassAndStreamAndStatus(
                schoolId, examinationId, classId, streamId, PUBLISHABLE_STATUSES);

        Map<Long, List<Result>> resultsByStudent = results.stream()
                .collect(Collectors.groupingBy(Result::getStudentId));

        List<Long> studentIds = new ArrayList<>(resultsByStudent.keySet());
        List<StudentProfile> students = studentProfileRepository.findBySchoolIdAndIdIn(schoolId, studentIds);
        Map<Long, StudentProfile> studentMap = students.stream()
                .collect(Collectors.toMap(StudentProfile::getId, s -> s));

        List<StudentResultPreview> studentPreviews = new ArrayList<>();
        List<ParentGuardianPreview> parentPreviews = new ArrayList<>();
        Set<String> seenParentPhones = new LinkedHashSet<>();

        for (Map.Entry<Long, List<Result>> entry : resultsByStudent.entrySet()) {
            Long studentId = entry.getKey();
            StudentProfile student = studentMap.get(studentId);
            if (student == null) continue;

            String studentName = fullName(student);
            String admissionNumber = student.getAdmissionNumber();
            String phone = student.getPhone();

            boolean studentEligible = false;
            String exclusionReason = null;

            if (phone == null || phone.isBlank()) {
                exclusionReason = "No phone number on file";
            } else {
                try {
                    darsmsProviderService.normalizeTanzanianPhone(phone);
                    studentEligible = true;
                } catch (NotificationProviderException e) {
                    exclusionReason = "Invalid phone number format";
                }
            }

            String sampleSubject = resultsByStudent.get(studentId).stream()
                    .map(this::subjectSummary)
                    .findFirst().orElse("");

            studentPreviews.add(new StudentResultPreview(
                    null, studentId, studentName, admissionNumber,
                    clazz.getId(), clazz.getName(),
                    streamId, streamName,
                    null, null, null,
                    null, null, null,
                    null, phone, studentEligible, exclusionReason
            ));
        }

        if ("PARENTS".equalsIgnoreCase(recipientType) || "BOTH".equalsIgnoreCase(recipientType)) {
            for (Map.Entry<Long, List<Result>> entry : resultsByStudent.entrySet()) {
                Long studentId = entry.getKey();
                StudentProfile student = studentMap.get(studentId);
                if (student == null) continue;

                String studentName = fullName(student);
                String admissionNumber = student.getAdmissionNumber();

                List<StudentParent> relationships = studentParentRepository.findByStudentId(studentId);
                List<StudentParent> smsRelationships = relationships.stream()
                        .filter(StudentParent::isReceivesSms)
                        .filter(rp -> !rp.isDeleted())
                        .toList();

                List<Parent> eligibleParents = new ArrayList<>();
                for (StudentParent rel : smsRelationships) {
                    Optional<Parent> parentOpt = parentRepository.findBySchoolIdAndId(schoolId, rel.getParentId());
                    if (parentOpt.isEmpty()) continue;
                    Parent parent = parentOpt.get();
                    if (parent.isDeleted()) continue;
                    eligibleParents.add(parent);
                }

                eligibleParents.sort((a, b) -> {
                    boolean aPrimary = smsRelationships.stream()
                            .anyMatch(rp -> rp.getParentId().equals(a.getId()) && rp.isPrimary());
                    boolean bPrimary = smsRelationships.stream()
                            .anyMatch(rp -> rp.getParentId().equals(b.getId()) && rp.isPrimary());
                    if (aPrimary && !bPrimary) return -1;
                    if (!aPrimary && bPrimary) return 1;
                    return 0;
                });

                for (Parent parent : eligibleParents) {
                    String phone = parent.getPhone();
                    if (phone == null || phone.isBlank()) {
                        parentPreviews.add(new ParentGuardianPreview(
                                parent.getId(), parentFullName(parent), null, studentId,
                                studentName, admissionNumber, null, false, "No phone number on file"));
                        continue;
                    }
                    try {
                        String normalized = darsmsProviderService.normalizeTanzanianPhone(phone);
                        if (!seenParentPhones.add(normalized)) {
                            parentPreviews.add(new ParentGuardianPreview(
                                    parent.getId(), parentFullName(parent), normalized, studentId,
                                    studentName, admissionNumber, null, false, "Duplicate phone number"));
                            continue;
                        }
                        parentPreviews.add(new ParentGuardianPreview(
                                parent.getId(), parentFullName(parent), normalized, studentId,
                                studentName, admissionNumber, null, true, null));
                    } catch (NotificationProviderException e) {
                        parentPreviews.add(new ParentGuardianPreview(
                                parent.getId(), parentFullName(parent), phone, studentId,
                                studentName, admissionNumber, null, false, "Invalid phone number format"));
                    }
                }
            }
        }

        int eligibleStudents = (int) studentPreviews.stream().filter(StudentResultPreview::eligible).count();
        int eligibleParents = (int) parentPreviews.stream().filter(ParentGuardianPreview::eligible).count();

        int eligibleCount;
        int excludedCount;
        if ("STUDENTS".equalsIgnoreCase(recipientType)) {
            eligibleCount = eligibleStudents;
            excludedCount = studentPreviews.size() - eligibleStudents;
        } else if ("PARENTS".equalsIgnoreCase(recipientType)) {
            eligibleCount = eligibleParents;
            excludedCount = parentPreviews.size() - eligibleParents;
        } else {
            eligibleCount = eligibleStudents + eligibleParents;
            excludedCount = (studentPreviews.size() - eligibleStudents) + (parentPreviews.size() - eligibleParents);
        }

        StudentResultPreview sampleStudent = studentPreviews.isEmpty() ? null : studentPreviews.get(0);
        String sampleSubjectResults = "";
        if (sampleStudent != null) {
            List<Result> sampleResults = resultsByStudent.get(sampleStudent.studentId());
            sampleSubjectResults = subjectResults(sampleResults == null ? List.of() : sampleResults);
        }
        String smsTemplate = resultNotificationTemplateService.getTemplate(schoolId).smsTemplate();
        String sampleMessage = resultNotificationTemplateService.render(smsTemplate,
                new ResultNotificationTemplateService.TemplateContext(
                        sampleStudent == null ? "" : sampleStudent.studentName(),
                        sampleStudent == null ? "" : sampleStudent.admissionNumber(),
                        clazz.getName(), streamName, examination.getName(),
                        sampleSubjectResults, schoolName,
                        sampleStudent == null ? "" : sampleStudent.studentName()));

        boolean canSend = eligibleCount > 0;
        String reason = canSend ? null : "No eligible recipients with valid phone numbers found";

        return new ResultNotificationPreview(
                examinationId, examination.getName(),
                classId, clazz.getName(),
                streamId, streamName,
                recipientType,
                eligibleCount, excludedCount,
                studentPreviews, parentPreviews,
                sampleMessage,
                canSend, reason
        );
    }

    @Transactional
    public ResultNotificationDto send(Long schoolId, Long examinationId, Long classId,
                                      Long streamId, String recipientType, String idempotencyKey,
                                      Long initiatedBy) {
        validateScope(schoolId, examinationId, classId, streamId);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        Optional<ResultNotification> existing = notificationRepository
                .findBySchoolIdAndIdempotencyKey(schoolId, idempotencyKey);
        if (existing.isPresent()) {
            return mapToDto(existing.get());
        }

        Examination examination = examinationRepository.findBySchoolIdAndId(schoolId, examinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
        com.edumanage.classes.entity.Class clazz = classRepository.findBySchoolIdAndId(schoolId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        com.edumanage.streams.entity.Stream stream = null;
        String streamName = null;
        if (streamId != null) {
            stream = streamRepository.findBySchoolIdAndId(schoolId, streamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            streamName = stream.getName();
        }

        String schoolName = schoolRepository.findById(schoolId)
                .map(s -> s.getName() != null ? s.getName() : "School")
                .orElse("School");

        List<Result> results = resultRepository.findByExaminationAndClassAndStreamAndStatus(
                schoolId, examinationId, classId, streamId, PUBLISHABLE_STATUSES);

        Map<Long, List<Result>> resultsByStudent = results.stream()
                .collect(Collectors.groupingBy(Result::getStudentId));

        List<Long> studentIds = new ArrayList<>(resultsByStudent.keySet());
        List<StudentProfile> students = studentProfileRepository.findBySchoolIdAndIdIn(schoolId, studentIds);
        Map<Long, StudentProfile> studentMap = students.stream()
                .collect(Collectors.toMap(StudentProfile::getId, s -> s));

        ResultNotification notification = new ResultNotification();
        notification.setSchoolId(schoolId);
        notification.setExaminationId(examinationId);
        notification.setClassId(classId);
        notification.setStreamId(streamId);
        notification.setRecipientType(recipientType.toUpperCase());
        notification.setIdempotencyKey(idempotencyKey);
        notification.setTitle("Results Notification - " + examination.getName() + " - " + clazz.getName()
                + (streamName != null ? " " + streamName : ""));
        notification.setStatus("PENDING");
        notification.setInitiatedBy(initiatedBy);
        notification = notificationRepository.saveAndFlush(notification);

        String smsTemplate = resultNotificationTemplateService.getTemplate(schoolId).smsTemplate();

        List<ResultNotificationDelivery> deliveries = new ArrayList<>();
        int totalRecipients = 0;
        int excludedCount = 0;
        Set<String> seenParentPhones = new LinkedHashSet<>();

        if ("STUDENTS".equalsIgnoreCase(recipientType) || "BOTH".equalsIgnoreCase(recipientType)) {
            for (Map.Entry<Long, List<Result>> entry : resultsByStudent.entrySet()) {
                Long studentId = entry.getKey();
                StudentProfile student = studentMap.get(studentId);
                if (student == null) continue;

                String phone = student.getPhone();
                boolean eligible;
                String exclusionReason;

                if (phone == null || phone.isBlank()) {
                    eligible = false;
                    exclusionReason = "No phone number on file";
                } else {
                    try {
                        darsmsProviderService.normalizeTanzanianPhone(phone);
                        eligible = true;
                        exclusionReason = null;
                    } catch (NotificationProviderException e) {
                        eligible = false;
                        exclusionReason = "Invalid phone number format";
                    }
                }

                totalRecipients++;
                if (!eligible) {
                    excludedCount++;
                    deliveries.add(buildExcludedDelivery(notification.getId(), "STUDENT", studentId,
                            fullName(student), phone, student.getAdmissionNumber(), exclusionReason));
                    continue;
                }

                String message = resultNotificationTemplateService.render(smsTemplate,
                        new ResultNotificationTemplateService.TemplateContext(
                                fullName(student), student.getAdmissionNumber(),
                                clazz.getName(), streamName, examination.getName(),
                                subjectResults(entry.getValue()), schoolName,
                                fullName(student)));

                try {
                    CreateSmsMessageRequest request = new CreateSmsMessageRequest();
                    request.setSchoolId(schoolId);
                    request.setRecipient(phone);
                    request.setMessage(message);
                    request.setBulk(true);
                    SmsMessageDto sent = smsMessageService.sendSms(request);
                    deliveries.add(buildPendingDelivery(notification.getId(), sent.getId(), "STUDENT", studentId,
                            fullName(student), phone, student.getAdmissionNumber()));
                } catch (Exception e) {
                    deliveries.add(buildFailedDelivery(notification.getId(), "STUDENT", studentId,
                            fullName(student), phone, student.getAdmissionNumber(), e.getMessage()));
                }
            }
        }

        if ("PARENTS".equalsIgnoreCase(recipientType) || "BOTH".equalsIgnoreCase(recipientType)) {
            for (Map.Entry<Long, List<Result>> entry : resultsByStudent.entrySet()) {
                Long studentId = entry.getKey();
                StudentProfile student = studentMap.get(studentId);
                if (student == null) continue;

                List<StudentParent> relationships = studentParentRepository.findByStudentId(studentId);
                List<StudentParent> smsRelationships = relationships.stream()
                        .filter(StudentParent::isReceivesSms)
                        .filter(rp -> !rp.isDeleted())
                        .toList();

                List<Parent> eligibleParents = new ArrayList<>();
                for (StudentParent rel : smsRelationships) {
                    Optional<Parent> parentOpt = parentRepository.findBySchoolIdAndId(schoolId, rel.getParentId());
                    if (parentOpt.isEmpty()) continue;
                    Parent parent = parentOpt.get();
                    if (parent.isDeleted()) continue;
                    eligibleParents.add(parent);
                }

                eligibleParents.sort((a, b) -> {
                    boolean aPrimary = smsRelationships.stream()
                            .anyMatch(rp -> rp.getParentId().equals(a.getId()) && rp.isPrimary());
                    boolean bPrimary = smsRelationships.stream()
                            .anyMatch(rp -> rp.getParentId().equals(b.getId()) && rp.isPrimary());
                    if (aPrimary && !bPrimary) return -1;
                    if (!aPrimary && bPrimary) return 1;
                    return 0;
                });

                for (Parent parent : eligibleParents) {
                    String phone = parent.getPhone();
                    if (phone == null || phone.isBlank()) {
                        totalRecipients++;
                        excludedCount++;
                        deliveries.add(buildExcludedDelivery(notification.getId(), "PARENT", parent.getId(),
                                parentFullName(parent), phone, student.getAdmissionNumber(), "No phone number on file"));
                        continue;
                    }
                    String normalized;
                    try {
                        normalized = darsmsProviderService.normalizeTanzanianPhone(phone);
                    } catch (NotificationProviderException e) {
                        totalRecipients++;
                        excludedCount++;
                        deliveries.add(buildExcludedDelivery(notification.getId(), "PARENT", parent.getId(),
                                parentFullName(parent), phone, student.getAdmissionNumber(), "Invalid phone number format"));
                        continue;
                    }
                    if (!seenParentPhones.add(normalized)) {
                        totalRecipients++;
                        excludedCount++;
                        deliveries.add(buildExcludedDelivery(notification.getId(), "PARENT", parent.getId(),
                                parentFullName(parent), normalized, student.getAdmissionNumber(),
                                "Duplicate phone number"));
                        continue;
                    }

                    totalRecipients++;
                    String message = resultNotificationTemplateService.render(smsTemplate,
                            new ResultNotificationTemplateService.TemplateContext(
                                    fullName(student), student.getAdmissionNumber(),
                                    clazz.getName(), streamName, examination.getName(),
                                    subjectResults(entry.getValue()), schoolName,
                                    parentFullName(parent)));

                    try {
                        CreateSmsMessageRequest request = new CreateSmsMessageRequest();
                        request.setSchoolId(schoolId);
                        request.setRecipient(normalized);
                        request.setMessage(message);
                        request.setBulk(true);
                        SmsMessageDto sent = smsMessageService.sendSms(request);
                        deliveries.add(buildPendingDelivery(notification.getId(), sent.getId(), "PARENT", parent.getId(),
                                parentFullName(parent), normalized, student.getAdmissionNumber()));
                        deliveries.get(deliveries.size() - 1).setStudentId(studentId);
                        deliveries.get(deliveries.size() - 1).setStudentName(fullName(student));
                    } catch (Exception e) {
                        deliveries.add(buildFailedDelivery(notification.getId(), "PARENT", parent.getId(),
                                parentFullName(parent), normalized, student.getAdmissionNumber(), e.getMessage()));
                        deliveries.get(deliveries.size() - 1).setStudentId(studentId);
                        deliveries.get(deliveries.size() - 1).setStudentName(fullName(student));
                    }
                }
            }
        }

        if (totalRecipients > MAX_RECIPIENTS) {
            throw new BadRequestException("Recipient count exceeds the maximum of " + MAX_RECIPIENTS);
        }

        notification.setTotalRecipients(totalRecipients);
        notification.setExcludedCount(excludedCount);
        notificationRepository.save(notification);

        deliveryRepository.saveAllAndFlush(deliveries);

        auditLogService.log("RESULT_NOTIFICATION_SEND", "ResultNotification", notification.getId(),
                "Bulk results notification sent; recipients=" + totalRecipients + ", excluded=" + excludedCount,
                null, null, null);

        refreshStatus(notification.getId());
        return mapToDto(notificationRepository.findById(notification.getId()).orElseThrow());
    }

    @Transactional
    public ResultNotificationDto retryFailed(Long schoolId, Long notificationId, Long initiatedBy) {
        ResultNotification notification = notificationRepository.findBySchoolIdAndId(schoolId, notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Result notification not found"));

        List<ResultNotificationDelivery> failed = deliveryRepository.findByNotificationIdAndStatus(notificationId, "FAILED");

        for (ResultNotificationDelivery delivery : failed) {
            if (delivery.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
                continue;
            }
            if (PERMANENT_FAILURE_CODES.contains(delivery.getFailureCode())) {
                continue;
            }
            if (deliveryRepository.markPendingDeliveryForRetry(delivery.getId()) != 1) {
                continue;
            }

            String phone = delivery.getRecipientPhone();
            String message = buildRetryMessage(notification, delivery);

            try {
                CreateSmsMessageRequest request = new CreateSmsMessageRequest();
                request.setSchoolId(schoolId);
                request.setRecipient(phone);
                request.setMessage(message);
                request.setBulk(true);
                SmsMessageDto sent = smsMessageService.sendSms(request);

                delivery.setStatus("SENT");
                delivery.setSmsMessageId(sent.getId());
                delivery.setProviderMessageId(sent.getProviderMessageId());
                delivery.setBatchId(sent.getBatchId());
                delivery.setFailureCode(null);
                delivery.setErrorMessage(null);
                delivery.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                delivery.setStatus("FAILED");
                delivery.setFailureCode("SMS_PROVIDER_UNAVAILABLE");
                delivery.setErrorMessage(e.getMessage());
                delivery.setAttemptCount(delivery.getAttemptCount() + 1);
                delivery.setFailedAt(LocalDateTime.now());
            }
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            delivery.setLastAttemptAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
        }

        refreshStatus(notification.getId());
        auditLogService.log("RESULT_NOTIFICATION_RETRY", "ResultNotification", notification.getId(),
                "Eligible failed deliveries retried", null, null, null);
        return mapToDto(notification);
    }

    private String buildRetryMessage(ResultNotification notification, ResultNotificationDelivery delivery) {
        return "Your " + notification.getTitle() + " result update is available. Please check the school portal for details.";
    }

    public Page<ResultNotificationDto> list(Long schoolId, Pageable pageable) {
        Page<ResultNotification> page = notificationRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable);
        return page.map(this::mapToDto);
    }

    public List<ResultNotificationDeliveryDto> getDeliveries(Long schoolId, Long notificationId) {
        ResultNotification notification = notificationRepository.findBySchoolIdAndId(schoolId, notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Result notification not found"));
        List<ResultNotificationDelivery> deliveries = deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(notificationId);
        deliveries = reconcileDeliveries(schoolId, deliveries);
        return deliveries.stream().map(this::mapDeliveryToDto).toList();
    }

    public ResultNotificationDto getById(Long schoolId, Long notificationId) {
        ResultNotification notification = notificationRepository.findBySchoolIdAndId(schoolId, notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Result notification not found"));
        return mapToDto(notification);
    }

    public ResultNotificationSummary getSummary(Long schoolId, Long notificationId) {
        ResultNotification notification = notificationRepository.findBySchoolIdAndId(schoolId, notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Result notification not found"));
        List<ResultNotificationDelivery> deliveries = deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(notificationId);
        deliveries = reconcileDeliveries(schoolId, deliveries);
        int successful = (int) deliveries.stream().filter(d -> "SENT".equals(d.getStatus()) || "DELIVERED".equals(d.getStatus())).count();
        int failed = (int) deliveries.stream().filter(d -> "FAILED".equals(d.getStatus())).count();
        int pending = (int) deliveries.stream().filter(d -> "PENDING".equals(d.getStatus())).count();
        int excluded = (int) deliveries.stream().filter(d -> "EXCLUDED".equals(d.getStatus())).count();
        return new ResultNotificationSummary(
                deliveries.size(), excluded, successful, failed, pending);
    }

    /**
     * Synchronizes a result notification delivery from the linked SMS message status.
     * Only forward transitions are applied:
     *   PENDING/RETRYING -> SENT/DELIVERED/FAILED
     *   SENT -> DELIVERED (genuine delivery receipt)
     *
     * A successful provider response never leaves the delivery PENDING.
     * No SMS is sent. Idempotent and strictly school-scoped: a delivery can only be
     * updated by its own school.
     */
    @Transactional
    public ResultNotificationDelivery syncDeliveryFromSmsMessage(Long schoolId, Long smsMessageId) {
        SmsMessage message = smsMessageRepository.findBySchoolIdAndId(schoolId, smsMessageId)
                .orElseThrow(() -> new ResourceNotFoundException("SMS message not found or not accessible for this school"));

        ResultNotificationDelivery delivery = deliveryRepository.findBySmsMessageId(smsMessageId)
                .orElseThrow(() -> new ResourceNotFoundException("No result notification delivery linked to SMS message " + smsMessageId));

        ResultNotification notification = notificationRepository.findById(delivery.getNotificationId())
                .orElseThrow(() -> new ResourceNotFoundException("Result notification not found"));

        if (!notification.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Cannot update a delivery belonging to another school");
        }

        String mapped = mapToDeliveryStatus(message.getStatus());
        if (mapped == null || !shouldTransition(delivery.getStatus(), mapped)) {
            return delivery;
        }

        delivery.setStatus(mapped);
        delivery.setProviderMessageId(message.getProviderMessageId());
        delivery.setBatchId(message.getBatchId());
        delivery.setFailureCode("FAILED".equals(mapped) ? "SMS_PROVIDER_FAILED" : delivery.getFailureCode());
        delivery.setErrorMessage(message.getErrorMessage());

        LocalDateTime now = LocalDateTime.now();
        switch (mapped) {
            case "SENT" -> {
                if (delivery.getSentAt() == null) {
                    delivery.setSentAt(now);
                }
            }
            case "DELIVERED" -> {
                if (delivery.getDeliveredAt() == null) {
                    delivery.setDeliveredAt(now);
                }
            }
            case "FAILED" -> {
                if (delivery.getFailedAt() == null) {
                    delivery.setFailedAt(now);
                }
            }
        }

        deliveryRepository.save(delivery);
        refreshStatus(notification.getId());
        return delivery;
    }

    /**
     * Idempotent reconciliation used when an SMS was accepted by the provider but the
     * delivery record was never updated (for example after a database synchronization
     * failure). It re-syncs the delivery from the existing SmsMessage record without
     * sending the SMS a second time.
     */
    @Transactional
    public ResultNotificationDeliveryDto reconcileDelivery(Long schoolId, Long smsMessageId) {
        ResultNotificationDelivery synced = syncDeliveryFromSmsMessage(schoolId, smsMessageId);
        return mapDeliveryToDto(synced);
    }

    /**
     * Reconciles non-terminal deliveries against their linked SmsMessage records so that
     * a browser refresh always returns the real backend status. Sends no SMS.
     */
    private List<ResultNotificationDelivery> reconcileDeliveries(Long schoolId, List<ResultNotificationDelivery> deliveries) {
        List<ResultNotificationDelivery> result = new ArrayList<>(deliveries);
        for (int i = 0; i < result.size(); i++) {
            ResultNotificationDelivery delivery = result.get(i);
            if (delivery.getSmsMessageId() == null) {
                continue;
            }
            String status = delivery.getStatus();
            if ("DELIVERED".equals(status) || "FAILED".equals(status) || "EXCLUDED".equals(status)) {
                continue;
            }
            try {
                ResultNotificationDelivery synced = syncDeliveryFromSmsMessage(schoolId, delivery.getSmsMessageId());
                if (synced != null) {
                    result.set(i, synced);
                }
            } catch (Exception e) {
                log.warn("Failed to reconcile delivery {} (smsMessageId={}): {}", delivery.getId(), delivery.getSmsMessageId(), e.getMessage());
            }
        }
        return result;
    }

    /**
     * Maps an SmsMessage status to a ResultNotificationDelivery status.
     * Returns null when no delivery transition is required (e.g. still PENDING).
     */
    private String mapToDeliveryStatus(String smsStatus) {
        if (smsStatus == null) {
            return null;
        }
        return switch (smsStatus.toUpperCase()) {
            case "ACCEPTED", "SENT" -> "SENT";
            case "DELIVERED" -> "DELIVERED";
            case "FAILED" -> "FAILED";
            default -> null;
        };
    }

    /**
     * Allow only forward status transitions. Never overwrite a terminal or newer status
     * with an older asynchronous PENDING update, so duplicate callbacks cannot corrupt
     * counts.
     */
    private boolean shouldTransition(String current, String mapped) {
        if (mapped == null || mapped.equals(current)) {
            return false;
        }
        if ("DELIVERED".equals(current) || "EXCLUDED".equals(current) || "FAILED".equals(current)) {
            return false;
        }
        if ("DELIVERED".equals(mapped)) {
            return "PENDING".equals(current) || "RETRYING".equals(current) || "SENT".equals(current);
        }
        if ("SENT".equals(mapped)) {
            return "PENDING".equals(current) || "RETRYING".equals(current);
        }
        if ("FAILED".equals(mapped)) {
            return "PENDING".equals(current) || "RETRYING".equals(current);
        }
        return false;
    }

    private void refreshStatus(Long notificationId) {
        List<ResultNotificationDelivery> deliveries = deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(notificationId);
        long sent = deliveries.stream().filter(d -> "SENT".equals(d.getStatus()) || "DELIVERED".equals(d.getStatus())).count();
        long failed = deliveries.stream().filter(d -> "FAILED".equals(d.getStatus())).count();
        long pending = deliveries.stream().filter(d -> "PENDING".equals(d.getStatus())).count();
        long eligible = deliveries.stream().filter(d -> !"EXCLUDED".equals(d.getStatus())).count();

        Optional<ResultNotification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) return;
        ResultNotification notification = notificationOpt.get();

        notification.setSuccessfulCount((int) sent);
        notification.setFailedCount((int) failed);
        notification.setPendingCount((int) pending);

        String status;
        if (eligible == 0) {
            status = "NO RECIPIENTS";
        } else if (pending > 0) {
            status = "PENDING";
        } else if (failed == eligible) {
            status = "FAILED";
        } else if (sent == eligible) {
            status = "SENT";
        } else {
            status = "PARTIALLY SENT";
        }
        notification.setStatus(status);
        notificationRepository.save(notification);
    }

    private String subjectSummary(Result result) {
        StringBuilder sb = new StringBuilder();
        sb.append(subjectName(result));
        sb.append(" ");
        sb.append(result.getMarksObtained() != null ? result.getMarksObtained() : "N/A");
        sb.append("/");
        sb.append(result.getExaminationSubject().getMaximumMarks() != null
                ? result.getExaminationSubject().getMaximumMarks() : "N/A");
        if (result.getGrade() != null && !result.getGrade().isBlank()) {
            sb.append(" (").append(result.getGrade()).append(")");
        }
        return sb.toString();
    }

    private String subjectResults(List<Result> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        return results.stream()
                .map(this::subjectSummary)
                .collect(Collectors.joining(", "));
    }

    private String subjectName(Result result) {
        if (result.getExaminationSubject() != null && result.getExaminationSubject().getSubject() != null) {
            return result.getExaminationSubject().getSubject().getName();
        }
        return "Subject";
    }

    private String fullName(StudentProfile student) {
        StringBuilder sb = new StringBuilder();
        if (student.getFirstName() != null) sb.append(student.getFirstName().trim());
        if (student.getMiddleName() != null && !student.getMiddleName().isBlank()) {
            sb.append(" ").append(student.getMiddleName().trim());
        }
        if (student.getLastName() != null) sb.append(" ").append(student.getLastName().trim());
        return sb.toString().trim();
    }

    private String parentFullName(Parent parent) {
        StringBuilder sb = new StringBuilder();
        if (parent.getFirstName() != null) sb.append(parent.getFirstName().trim());
        if (parent.getLastName() != null) sb.append(" ").append(parent.getLastName().trim());
        return sb.toString().trim();
    }

    private ResultNotificationDelivery buildExcludedDelivery(Long notificationId, String recipientType,
                                                              Long recipientId, String recipientName,
                                                              String phone, String admissionNumber, String reason) {
        ResultNotificationDelivery d = new ResultNotificationDelivery();
        d.setNotificationId(notificationId);
        d.setRecipientType(recipientType);
        d.setRecipientId(recipientId);
        d.setRecipientName(recipientName);
        d.setRecipientPhone(phone);
        d.setAdmissionNumber(admissionNumber);
        d.setChannel("SMS");
        d.setStatus("EXCLUDED");
        d.setFailureCode("INVALID_PHONE");
        d.setErrorMessage(reason);
        d.setAttemptCount(0);
        d.setLastAttemptAt(LocalDateTime.now());
        return d;
    }

    private ResultNotificationDelivery buildPendingDelivery(Long notificationId, Long smsMessageId,
                                                             String recipientType, Long recipientId,
                                                             String recipientName, String phone, String admissionNumber) {
        ResultNotificationDelivery d = new ResultNotificationDelivery();
        d.setNotificationId(notificationId);
        d.setSmsMessageId(smsMessageId);
        d.setRecipientType(recipientType);
        d.setRecipientId(recipientId);
        d.setRecipientName(recipientName);
        d.setRecipientPhone(phone);
        d.setAdmissionNumber(admissionNumber);
        d.setChannel("SMS");
        d.setStatus("PENDING");
        d.setAttemptCount(0);
        d.setLastAttemptAt(LocalDateTime.now());
        return d;
    }

    private ResultNotificationDelivery buildFailedDelivery(Long notificationId, String recipientType,
                                                            Long recipientId, String recipientName,
                                                            String phone, String admissionNumber, String error) {
        ResultNotificationDelivery d = new ResultNotificationDelivery();
        d.setNotificationId(notificationId);
        d.setRecipientType(recipientType);
        d.setRecipientId(recipientId);
        d.setRecipientName(recipientName);
        d.setRecipientPhone(phone);
        d.setAdmissionNumber(admissionNumber);
        d.setChannel("SMS");
        d.setStatus("FAILED");
        d.setFailureCode("SMS_PROVIDER_UNAVAILABLE");
        d.setErrorMessage(error);
        d.setAttemptCount(1);
        d.setLastAttemptAt(LocalDateTime.now());
        return d;
    }

    private ResultNotificationDto mapToDto(ResultNotification notification) {
        return new ResultNotificationDto(
                notification.getId(),
                notification.getUuid() != null ? notification.getUuid().toString() : null,
                notification.getSchoolId(),
                notification.getExaminationId(),
                notification.getClassId(),
                notification.getStreamId(),
                notification.getRecipientType(),
                notification.getIdempotencyKey(),
                notification.getTitle(),
                notification.getTotalRecipients(),
                notification.getExcludedCount(),
                notification.getSuccessfulCount(),
                notification.getFailedCount(),
                notification.getPendingCount(),
                notification.getStatus(),
                notification.getInitiatedBy(),
                notification.getCreatedAt()
        );
    }

    private ResultNotificationDeliveryDto mapDeliveryToDto(ResultNotificationDelivery delivery) {
        return new ResultNotificationDeliveryDto(
                delivery.getId(),
                delivery.getNotificationId(),
                delivery.getSmsMessageId(),
                delivery.getRecipientType(),
                delivery.getRecipientId(),
                delivery.getRecipientName(),
                delivery.getRecipientPhone(),
                delivery.getStudentId(),
                delivery.getStudentName(),
                delivery.getAdmissionNumber(),
                delivery.getChannel(),
                delivery.getStatus(),
                delivery.getProviderMessageId(),
                delivery.getBatchId(),
                delivery.getFailureCode(),
                delivery.getErrorMessage(),
                delivery.getDeliveryStatus(),
                delivery.getAttemptCount(),
                delivery.getLastAttemptAt(),
                delivery.getSentAt(),
                delivery.getDeliveredAt(),
                delivery.getFailedAt()
        );
    }
}
