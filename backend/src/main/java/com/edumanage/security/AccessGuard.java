package com.edumanage.security;

import com.edumanage.boarding.repository.BoardingStudentRepository;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.parents.repository.ParentRepository;
import com.edumanage.studentparents.repository.StudentParentRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.transport.repository.TransportAssignmentRepository;
import com.edumanage.users.entity.User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Central authorisation helper for role restricted school data.
 *
 * <p>The guard answers a single question: may the authenticated user read the
 * records of a given student? Roles that operate a whole school (manager,
 * platform administrator, accountant, head of department) may read every
 * student of their own school. Roles that only own a slice of the student
 * population (parent, student, teacher, warden, transport officer) are limited
 * to that slice.</p>
 */
@Service
public class AccessGuard {

    private static final Set<String> FINANCIAL_ROLES = Set.of("SUPER_ADMIN", "MANAGER", "ACCOUNTANT");
    private static final Set<String> LIBRARY_ROLES = Set.of("SUPER_ADMIN", "MANAGER", "LIBRARIAN");
    private static final Set<String> SCHOOL_WIDE_STUDENT_ROLES = Set.of("SUPER_ADMIN", "MANAGER", "ACCOUNTANT", "HOD");
    private static final Set<String> RESTRICTED_LIST_ROLES = Set.of("PARENT", "STUDENT");

    private final CurrentUserService currentUserService;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final BoardingStudentRepository boardingStudentRepository;
    private final TransportAssignmentRepository transportAssignmentRepository;

    public AccessGuard(CurrentUserService currentUserService,
                       ParentRepository parentRepository,
                       StudentParentRepository studentParentRepository,
                       StudentProfileRepository studentProfileRepository,
                       TeacherAssignmentRepository teacherAssignmentRepository,
                       StudentEnrollmentRepository studentEnrollmentRepository,
                       BoardingStudentRepository boardingStudentRepository,
                       TransportAssignmentRepository transportAssignmentRepository) {
        this.currentUserService = currentUserService;
        this.parentRepository = parentRepository;
        this.studentParentRepository = studentParentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.boardingStudentRepository = boardingStudentRepository;
        this.transportAssignmentRepository = transportAssignmentRepository;
    }

    public void assertFinancialAccess() {
        User user = currentUserService.requireCurrentUser();
        if (currentUserService.rolesOf(user).stream().noneMatch(FINANCIAL_ROLES::contains)) {
            throw new ForbiddenException("You are not authorised to access financial records");
        }
    }

    /**
     * Ensures the authenticated user holds at least one of the supplied roles.
     */
    public void assertAnyRole(String... roles) {
        User user = currentUserService.requireCurrentUser();
        if (!currentUserService.hasAnyRole(user, roles)) {
            throw new ForbiddenException("You are not authorised to access this resource");
        }
    }

    public void assertLibraryAccess() {
        User user = currentUserService.requireCurrentUser();
        if (currentUserService.rolesOf(user).stream().noneMatch(LIBRARY_ROLES::contains)) {
            throw new ForbiddenException("You are not authorised to access library records");
        }
    }

    /**
     * Blocks bulk listings of every student in the school for roles that must
     * only ever see their own records.
     */
    public void assertCanListAllStudents() {
        User user = currentUserService.requireCurrentUser();
        Set<String> roles = currentUserService.rolesOf(user);
        if (roles.stream().anyMatch(RESTRICTED_LIST_ROLES::contains)) {
            throw new ForbiddenException("Access denied");
        }
    }

    public boolean canAccessStudent(Long studentId) {
        if (studentId == null) {
            return false;
        }

        User user = currentUserService.requireCurrentUser();
        Set<String> roles = currentUserService.rolesOf(user);

        if (roles.stream().anyMatch(SCHOOL_WIDE_STUDENT_ROLES::contains)) {
            return true;
        }
        if (roles.contains("PARENT")) {
            return linkedChildIds(user).contains(studentId);
        }
        if (roles.contains("STUDENT")) {
            Long ownStudentId = ownStudentId(user);
            return ownStudentId != null && ownStudentId.equals(studentId);
        }
        if (roles.contains("TEACHER")) {
            return assignedStudentIds(user).contains(studentId);
        }
        if (roles.contains("WARDEN")) {
            return boardingStudentIds(user).contains(studentId);
        }
        if (roles.contains("TRANSPORT_OFFICER")) {
            return transportStudentIds(user).contains(studentId);
        }
        return false;
    }

    public void assertCanAccessStudent(Long studentId) {
        if (!canAccessStudent(studentId)) {
            throw new ForbiddenException("Access denied");
        }
    }

    /**
     * A parent may only read the relations of the parent record linked to the
     * authenticated account. Manager and platform roles may read any parent of
     * their own school.
     */
    public void assertCanAccessParent(Long parentId) {
        User user = currentUserService.requireCurrentUser();
        Set<String> roles = currentUserService.rolesOf(user);

        if (roles.contains("SUPER_ADMIN") || roles.contains("MANAGER")) {
            return;
        }
        if (roles.contains("PARENT")) {
            Long linkedParentId = currentParentId(user);
            if (linkedParentId != null && linkedParentId.equals(parentId)) {
                return;
            }
        }
        throw new ForbiddenException("Access denied");
    }

    /**
     * @return the identifiers of every student the current user may read.
     */
    public Set<Long> accessibleStudentIds() {
        User user = currentUserService.requireCurrentUser();
        Set<String> roles = currentUserService.rolesOf(user);

        if (roles.stream().anyMatch(SCHOOL_WIDE_STUDENT_ROLES::contains)) {
            return Set.of();
        }
        if (roles.contains("PARENT")) {
            return linkedChildIds(user);
        }
        if (roles.contains("STUDENT")) {
            Long ownStudentId = ownStudentId(user);
            return ownStudentId == null ? Set.of() : Set.of(ownStudentId);
        }
        if (roles.contains("TEACHER")) {
            return assignedStudentIds(user);
        }
        if (roles.contains("WARDEN")) {
            return boardingStudentIds(user);
        }
        if (roles.contains("TRANSPORT_OFFICER")) {
            return transportStudentIds(user);
        }
        return Set.of();
    }

    /**
     * @return the parent profile linked to the authenticated user, or {@code null}
     * when the account is not linked to a parent record.
     */
    public Long currentParentId(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        return parentRepository.findByUserId(user.getId())
                .filter(parent -> !parent.isDeleted())
                .filter(parent -> user.getSchoolId() == null || user.getSchoolId().equals(parent.getSchoolId()))
                .map(com.edumanage.parents.entity.Parent::getId)
                .orElse(null);
    }

    private Set<Long> linkedChildIds(User user) {
        Long parentId = currentParentId(user);
        if (parentId == null) {
            return Set.of();
        }
        Set<Long> studentIds = new HashSet<>();
        studentParentRepository.findByParentId(parentId)
                .stream()
                .filter(relation -> !relation.isDeleted())
                .forEach(relation -> studentIds.add(relation.getStudentId()));
        return studentIds;
    }

    private Long ownStudentId(User user) {
        return studentProfileRepository.findByUserId(user.getId())
                .filter(profile -> !profile.isDeleted())
                .map(com.edumanage.studentprofiles.entity.StudentProfile::getId)
                .orElse(null);
    }

    private Set<Long> assignedStudentIds(User user) {
        Set<Long> studentIds = new HashSet<>();
        if (user.getSchoolId() == null) {
            return studentIds;
        }
        teacherAssignmentRepository.findByTeacherIdAndSchoolId(user.getId(), user.getSchoolId())
                .stream()
                .filter(assignment -> !assignment.isDeleted())
                .map(assignment -> assignment.getClassId())
                .distinct()
                .forEach(classId -> studentEnrollmentRepository.findBySchoolIdAndClassId(user.getSchoolId(), classId)
                        .forEach(enrollment -> studentIds.add(enrollment.getStudentId())));
        return studentIds;
    }

    private Set<Long> boardingStudentIds(User user) {
        Set<Long> studentIds = new HashSet<>();
        if (user.getSchoolId() == null) {
            return studentIds;
        }
        boardingStudentRepository.findBySchoolIdAndStatus(user.getSchoolId(), "ACTIVE")
                .forEach(boardingStudent -> studentIds.add(boardingStudent.getStudentId()));
        return studentIds;
    }

    private Set<Long> transportStudentIds(User user) {
        Set<Long> studentIds = new HashSet<>();
        if (user.getSchoolId() == null) {
            return studentIds;
        }
        transportAssignmentRepository.findBySchoolIdAndStatus(user.getSchoolId(), "ACTIVE")
                .forEach(assignment -> studentIds.add(assignment.getStudentId()));
        return studentIds;
    }
}
