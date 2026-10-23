package com.edumanage.dashboards.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.attendance.entity.Attendance;
import com.edumanage.attendance.repository.AttendanceRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.dashboards.dto.ParentDashboardDto;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.fees.entity.FeeAssignment;
import com.edumanage.fees.entity.FeeStructure;
import com.edumanage.fees.entity.Payment;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.fees.repository.FeeStructureRepository;
import com.edumanage.fees.repository.PaymentRepository;
import com.edumanage.notifications.entity.Notification;
import com.edumanage.notifications.repository.NotificationRepository;
import com.edumanage.parents.entity.Parent;
import com.edumanage.parents.repository.ParentRepository;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.CurrentUserService;
import com.edumanage.studentparents.repository.StudentParentRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.users.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Builds the parent dashboard.
 *
 * <p>Every figure is derived from the children that are linked to the
 * authenticated parent account. The service never accepts a child identifier
 * from the caller, so a parent can not reach another family's records by
 * supplying an arbitrary student id.</p>
 */
@Service
public class ParentDashboardService {

    private static final int RECENT_LIMIT = 10;
    private static final int NOTIFICATION_LIMIT = 6;
    private static final List<String> RELEASED_RESULT_STATUSES = List.of("APPROVED", "PUBLISHED");

    private final CurrentUserService currentUserService;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AttendanceRepository attendanceRepository;
    private final ResultRepository resultRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final SchoolRepository schoolRepository;

    public ParentDashboardService(CurrentUserService currentUserService,
                                  ParentRepository parentRepository,
                                  StudentParentRepository studentParentRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  StudentEnrollmentRepository studentEnrollmentRepository,
                                  ClassRepository classRepository,
                                  StreamRepository streamRepository,
                                  SubjectRepository subjectRepository,
                                  AcademicYearRepository academicYearRepository,
                                  AttendanceRepository attendanceRepository,
                                  ResultRepository resultRepository,
                                  FeeAssignmentRepository feeAssignmentRepository,
                                  FeeStructureRepository feeStructureRepository,
                                  PaymentRepository paymentRepository,
                                  NotificationRepository notificationRepository,
                                  SchoolRepository schoolRepository) {
        this.currentUserService = currentUserService;
        this.parentRepository = parentRepository;
        this.studentParentRepository = studentParentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.subjectRepository = subjectRepository;
        this.academicYearRepository = academicYearRepository;
        this.attendanceRepository = attendanceRepository;
        this.resultRepository = resultRepository;
        this.feeAssignmentRepository = feeAssignmentRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.schoolRepository = schoolRepository;
    }

    @Transactional(readOnly = true)
    public ParentDashboardDto getDashboard() {
        User user = currentUserService.requireCurrentUser();
        Long schoolId = user.getSchoolId();
        if (schoolId == null) {
            throw new ForbiddenException("A school assignment is required");
        }

        ParentDashboardDto dto = new ParentDashboardDto();
        dto.setSchoolId(schoolId);
        dto.setSchoolName(schoolRepository.findById(schoolId).map(school -> school.getName()).orElse(null));
        dto.setParentName(currentUserService.displayName(user));
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());

        AcademicYear academicYear = academicYearRepository.findBySchoolIdAndIsCurrentTrueAndDeletedAtIsNull(schoolId).orElse(null);
        if (academicYear != null) {
            dto.setAcademicYearId(academicYear.getId());
            dto.setAcademicYearName(academicYear.getName());
        }

        Optional<Parent> linkedParent = resolveParent(user, schoolId);
        if (linkedParent.isEmpty()) {
            dto.setProfileLinked(false);
            dto.setNotifications(recentNotifications(user, schoolId));
            dto.setUnreadNotificationCount(notificationRepository.countByUserIdAndSchoolIdAndIsReadFalse(user.getId(), schoolId));
            return dto;
        }

        Parent parent = linkedParent.get();
        dto.setProfileLinked(true);
        dto.setParentId(parent.getId());
        dto.setParentName(parent.getFirstName() + " " + parent.getLastName());
        dto.setEmail(parent.getEmail() != null ? parent.getEmail() : user.getEmail());
        dto.setPhone(parent.getPhone() != null ? parent.getPhone() : user.getPhone());

        List<ParentDashboardDto.Child> children = new ArrayList<>();
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (StudentProfile student : linkedChildren(parent, schoolId)) {
            ParentDashboardDto.Child child = buildChild(student, schoolId, academicYear);
            totalOutstanding = totalOutstanding.add(child.getBalance());
            children.add(child);
        }

        dto.setChildren(children);
        dto.setChildCount(children.size());
        dto.setTotalOutstandingBalance(totalOutstanding);
        dto.setNotifications(recentNotifications(user, schoolId));
        dto.setUnreadNotificationCount(notificationRepository.countByUserIdAndSchoolIdAndIsReadFalse(user.getId(), schoolId));

        return dto;
    }

    private Optional<Parent> resolveParent(User user, Long schoolId) {
        Optional<Parent> byUser = parentRepository.findByUserId(user.getId())
                .filter(parent -> !parent.isDeleted())
                .filter(parent -> schoolId.equals(parent.getSchoolId()));
        if (byUser.isPresent()) {
            return byUser;
        }
        if (user.getEmail() == null) {
            return Optional.empty();
        }
        return parentRepository.findFirstBySchoolIdAndEmailIgnoreCase(schoolId, user.getEmail())
                .filter(parent -> !parent.isDeleted());
    }

    private List<StudentProfile> linkedChildren(Parent parent, Long schoolId) {
        List<StudentProfile> children = new ArrayList<>();
        studentParentRepository.findByParentId(parent.getId()).stream()
                .filter(relation -> !relation.isDeleted())
                .map(relation -> relation.getStudentId())
                .distinct()
                .forEach(studentId -> studentProfileRepository.findById(studentId)
                        .filter(student -> !student.isDeleted())
                        .filter(student -> schoolId.equals(student.getSchoolId()))
                        .ifPresent(children::add));
        children.sort(Comparator.comparing(student -> student.getFirstName() + " " + student.getLastName()));
        return children;
    }

    private ParentDashboardDto.Child buildChild(StudentProfile student, Long schoolId, AcademicYear academicYear) {
        ParentDashboardDto.Child child = new ParentDashboardDto.Child();
        child.setStudentId(student.getId());
        child.setStudentName(student.getFirstName() + " " + student.getLastName());
        child.setAdmissionNumber(student.getAdmissionNumber());
        child.setStudentType(student.getStudentType());
        child.setStatus(student.getStatus());

        StudentEnrollment enrollment = resolveEnrollment(student.getId(), schoolId, academicYear);
        if (enrollment != null) {
            child.setEnrollmentStatus(enrollment.getStatus());
            child.setClassName(classRepository.findById(enrollment.getClassId()).map(clazz -> clazz.getName()).orElse(null));
            if (enrollment.getStreamId() != null) {
                child.setStreamName(streamRepository.findById(enrollment.getStreamId()).map(stream -> stream.getName()).orElse(null));
            }
            child.setAcademicYearName(academicYearRepository.findById(enrollment.getAcademicYearId())
                    .map(year -> year.getName())
                    .orElse(child.getAcademicYearName()));
        }

        List<Attendance> attendanceRecords = attendanceRepository.findByStudentIdAndSchoolId(student.getId(), schoolId).stream()
                .filter(record -> !record.isDeleted())
                .sorted(Comparator.comparing(Attendance::getAttendanceDate).reversed())
                .toList();

        child.setAttendance(summariseAttendance(attendanceRecords));
        child.setRecentAttendance(attendanceRecords.stream()
                .limit(RECENT_LIMIT)
                .map(record -> new ParentDashboardDto.AttendanceRecord(
                        record.getAttendanceDate(),
                        record.getStatus(),
                        classRepository.findById(record.getClassId()).map(clazz -> clazz.getName()).orElse(null),
                        record.getSubjectId() == null ? null
                                : subjectRepository.findById(record.getSubjectId()).map(subject -> subject.getName()).orElse(null),
                        record.getRemarks()))
                .toList());

        child.setRecentResults(recentResults(student.getId(), schoolId));
        applyFees(child, student.getId(), schoolId);
        return child;
    }

    private ParentDashboardDto.AttendanceSummary summariseAttendance(List<Attendance> records) {
        ParentDashboardDto.AttendanceSummary summary = new ParentDashboardDto.AttendanceSummary();
        summary.setTotal(records.size());
        long present = 0;
        long absent = 0;
        long late = 0;
        long excused = 0;
        for (Attendance record : records) {
            String status = record.getStatus() == null ? "" : record.getStatus().trim().toUpperCase();
            switch (status) {
                case "PRESENT" -> present++;
                case "ABSENT" -> absent++;
                case "LATE" -> late++;
                case "EXCUSED" -> excused++;
                default -> {
                }
            }
        }
        summary.setPresent(present);
        summary.setAbsent(absent);
        summary.setLate(late);
        summary.setExcused(excused);
        if (records.isEmpty()) {
            summary.setAttendanceRate(null);
        } else {
            double attended = present + late;
            summary.setAttendanceRate(BigDecimal.valueOf(attended)
                    .divide(BigDecimal.valueOf(records.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue());
        }
        return summary;
    }

    private StudentEnrollment resolveEnrollment(Long studentId, Long schoolId, AcademicYear academicYear) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudentIdAndSchoolId(studentId, schoolId).stream()
                .filter(enrollment -> !enrollment.isDeleted())
                .toList();
        if (enrollments.isEmpty()) {
            return null;
        }
        if (academicYear != null) {
            Optional<StudentEnrollment> current = enrollments.stream()
                    .filter(enrollment -> academicYear.getId().equals(enrollment.getAcademicYearId()))
                    .findFirst();
            if (current.isPresent()) {
                return current.get();
            }
        }
        return enrollments.stream()
                .max(Comparator.comparing(StudentEnrollment::getStartDate))
                .orElse(null);
    }

    private List<ParentDashboardDto.Result> recentResults(Long studentId, Long schoolId) {
        return resultRepository.findByStudentIdAndSchoolId(studentId, schoolId,
                        PageRequest.of(0, RECENT_LIMIT * 3, Sort.by(Sort.Direction.DESC, "id")))
                .getContent().stream()
                .filter(result -> !result.isDeleted())
                .filter(result -> RELEASED_RESULT_STATUSES.contains(
                        result.getStatus() == null ? "" : result.getStatus().trim().toUpperCase()))
                .limit(RECENT_LIMIT)
                .map(result -> new ParentDashboardDto.Result(
                        result.getId(),
                        result.getExamination() == null ? null : result.getExamination().getName(),
                        result.getExaminationSubject() == null ? null
                                : result.getExaminationSubject().getSubject() == null ? null
                                : result.getExaminationSubject().getSubject().getName(),
                        result.getMarksObtained() == null ? null : result.getMarksObtained().intValue(),
                        result.getPercentage() == null ? null : result.getPercentage().doubleValue(),
                        result.getGrade(),
                        result.getStatus(),
                        result.getRemarks()))
                .toList();
    }

    private void applyFees(ParentDashboardDto.Child child, Long studentId, Long schoolId) {
        BigDecimal totalDue = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal balance = BigDecimal.ZERO;

        List<ParentDashboardDto.FeeItem> feeItems = new ArrayList<>();
        for (FeeAssignment assignment : feeAssignmentRepository.findBySchoolIdAndStudentId(schoolId, studentId)) {
            if (assignment.isDeleted()) {
                continue;
            }
            FeeStructure structure = feeStructureRepository.findById(assignment.getFeeStructureId()).orElse(null);
            BigDecimal amountDue = assignment.getAmountDue() == null ? BigDecimal.ZERO : assignment.getAmountDue();
            BigDecimal amountPaid = assignment.getAmountPaid() == null ? BigDecimal.ZERO : assignment.getAmountPaid();
            BigDecimal itemBalance = assignment.getBalance() == null ? BigDecimal.ZERO : assignment.getBalance();

            totalDue = totalDue.add(amountDue);
            totalPaid = totalPaid.add(amountPaid);
            balance = balance.add(itemBalance);

            feeItems.add(new ParentDashboardDto.FeeItem(
                    assignment.getId(),
                    structure == null ? "Fee" : structure.getName(),
                    amountDue,
                    amountPaid,
                    itemBalance,
                    assignment.getDueDate(),
                    assignment.getStatus()));
        }

        child.setTotalFeesDue(totalDue);
        child.setTotalFeesPaid(totalPaid);
        child.setBalance(balance);
        child.setFeeItems(feeItems);

        List<ParentDashboardDto.Payment> payments = paymentRepository.findBySchoolIdAndStudentId(schoolId, studentId).stream()
                .filter(payment -> !payment.isDeleted())
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed()
                        .thenComparing(Payment::getId, Comparator.reverseOrder()))
                .limit(RECENT_LIMIT)
                .map(payment -> new ParentDashboardDto.Payment(
                        payment.getId(),
                        payment.getAmount(),
                        payment.getPaymentMethod(),
                        payment.getPaymentDate(),
                        payment.getReceiptNumber(),
                        payment.getReferenceNumber(),
                        payment.getStatus()))
                .toList();
        child.setRecentPayments(payments);
    }

    private List<com.edumanage.notifications.dto.NotificationDto> recentNotifications(User user, Long schoolId) {
        return notificationRepository
                .findByUserIdAndSchoolIdOrderByCreatedAtDesc(user.getId(), schoolId,
                        PageRequest.of(0, NOTIFICATION_LIMIT))
                .getContent().stream()
                .filter(notification -> !notification.isDeleted())
                .map(this::mapNotification)
                .toList();
    }

    private com.edumanage.notifications.dto.NotificationDto mapNotification(Notification notification) {
        var dto = new com.edumanage.notifications.dto.NotificationDto();
        dto.setId(notification.getId());
        dto.setUuid(notification.getUuid() != null ? notification.getUuid().toString() : null);
        dto.setSchoolId(notification.getSchoolId());
        dto.setUserId(notification.getUserId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setNotificationType(notification.getNotificationType());
        dto.setPriority(notification.getPriority());
        dto.setIsRead(notification.getIsRead());
        dto.setReadAt(notification.getReadAt());
        dto.setActionUrl(notification.getActionUrl());
        dto.setMetadata(notification.getMetadata());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setUpdatedAt(notification.getUpdatedAt());
        return dto;
    }
}
