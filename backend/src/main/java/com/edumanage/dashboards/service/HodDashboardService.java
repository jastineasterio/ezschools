package com.edumanage.dashboards.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.dashboards.dto.HodDashboardDto;
import com.edumanage.departments.entity.Department;
import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.AccessGuard;
import com.edumanage.security.CurrentUserService;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.users.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the head of department dashboard. The overview is limited to the
 * academic content of the head's own school.
 */
@Service
public class HodDashboardService {

    private static final int MAX_ROWS = 500;
    private static final int EXAMINATION_LIMIT = 5;

    private final AccessGuard accessGuard;
    private final CurrentUserService currentUserService;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ResultRepository resultRepository;
    private final ExaminationRepository examinationRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolRepository schoolRepository;

    public HodDashboardService(AccessGuard accessGuard,
                               CurrentUserService currentUserService,
                               DepartmentRepository departmentRepository,
                               SubjectRepository subjectRepository,
                               TeacherAssignmentRepository teacherAssignmentRepository,
                               TeacherProfileRepository teacherProfileRepository,
                               StudentProfileRepository studentProfileRepository,
                               StudentEnrollmentRepository studentEnrollmentRepository,
                               ResultRepository resultRepository,
                               ExaminationRepository examinationRepository,
                               AcademicYearRepository academicYearRepository,
                               SchoolRepository schoolRepository) {
        this.accessGuard = accessGuard;
        this.currentUserService = currentUserService;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.resultRepository = resultRepository;
        this.examinationRepository = examinationRepository;
        this.academicYearRepository = academicYearRepository;
        this.schoolRepository = schoolRepository;
    }

    @Transactional(readOnly = true)
    public HodDashboardDto getDashboard() {
        accessGuard.assertAnyRole("HOD", "MANAGER", "SUPER_ADMIN");

        User user = currentUserService.requireCurrentUser();
        Long schoolId = user.getSchoolId();
        if (schoolId == null) {
            throw new com.edumanage.exception.ForbiddenException("A school assignment is required");
        }

        HodDashboardDto dto = new HodDashboardDto();
        dto.setSchoolId(schoolId);
        dto.setSchoolName(schoolRepository.findById(schoolId).map(school -> school.getName()).orElse(null));

        AcademicYear academicYear = academicYearRepository.findBySchoolIdAndIsCurrentTrueAndDeletedAtIsNull(schoolId).orElse(null);
        if (academicYear != null) {
            dto.setAcademicYearId(academicYear.getId());
            dto.setAcademicYearName(academicYear.getName());
        }

        List<Department> departments = departmentRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_ROWS)).getContent();
        dto.setTotalDepartments(departments.size());

        List<Department> headed = departmentRepository.findBySchoolIdAndHeadOfDepartmentId(schoolId, user.getId()).stream()
                .filter(department -> !department.isDeleted())
                .toList();
        dto.setHeadedDepartments(headed.size());

        List<Subject> subjects = subjectRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_ROWS)).getContent();
        Map<Long, Long> subjectCountByDepartment = new HashMap<>();
        Map<Long, Long> subjectById = new HashMap<>();
        for (Subject subject : subjects) {
            subjectById.put(subject.getId(), subject.getDepartmentId());
            subjectCountByDepartment.merge(subject.getDepartmentId(), 1L, Long::sum);
        }
        dto.setSubjects(subjects.size());

        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_ROWS)).getContent();
        Map<Long, Set<Long>> teachersByDepartment = new HashMap<>();
        Map<Long, Set<Long>> classesByDepartment = new HashMap<>();
        for (TeacherAssignment assignment : assignments) {
            if (assignment.isDeleted()) {
                continue;
            }
            Long departmentId = subjectById.get(assignment.getSubjectId());
            if (departmentId == null) {
                continue;
            }
            teachersByDepartment.computeIfAbsent(departmentId, key -> new HashSet<>()).add(assignment.getTeacherId());
            classesByDepartment.computeIfAbsent(departmentId, key -> new HashSet<>()).add(assignment.getClassId());
        }

        Map<Long, Set<Long>> studentsByClass = new HashMap<>();
        if (academicYear != null) {
            studentEnrollmentRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYear.getId()).stream()
                    .filter(enrollment -> !enrollment.isDeleted())
                    .forEach(enrollment -> studentsByClass
                            .computeIfAbsent(enrollment.getClassId(), key -> new HashSet<>())
                            .add(enrollment.getStudentId()));
        }

        List<HodDashboardDto.DepartmentSummary> summaries = new ArrayList<>();
        for (Department department : departments) {
            HodDashboardDto.DepartmentSummary summary = new HodDashboardDto.DepartmentSummary();
            summary.setId(department.getId());
            summary.setName(department.getName());
            summary.setCode(department.getCode());
            summary.setStatus(department.getStatus());
            summary.setSubjects(subjectCountByDepartment.getOrDefault(department.getId(), 0L));
            summary.setTeachers(teachersByDepartment.getOrDefault(department.getId(), Set.of()).size());
            Set<Long> students = new HashSet<>();
            for (Long classId : classesByDepartment.getOrDefault(department.getId(), Set.of())) {
                students.addAll(studentsByClass.getOrDefault(classId, Set.of()));
            }
            summary.setStudents(students.size());
            summaries.add(summary);
        }
        dto.setDepartments(summaries);

        dto.setTeachers(teacherProfileRepository.countBySchoolIdAndDeletedAtIsNull(schoolId));
        dto.setStudents(studentProfileRepository.countBySchoolIdAndDeletedAtIsNull(schoolId));
        dto.setPendingResultApprovals(resultRepository.countBySchoolIdAndStatus(schoolId, "SUBMITTED"));
        dto.setPublishedResults(resultRepository.countBySchoolIdAndStatusAndDeletedAtIsNull(schoolId, "PUBLISHED"));

        LocalDate today = LocalDate.now();
        dto.setUpcomingExaminations(examinationRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.ASC, "startDate")))
                .getContent().stream()
                .filter(examination -> !examination.isDeleted())
                .filter(examination -> examination.getStartDate() == null || !examination.getStartDate().isBefore(today))
                .limit(EXAMINATION_LIMIT)
                .map(examination -> new HodDashboardDto.ExaminationSummary(
                        examination.getId(),
                        examination.getName(),
                        examination.getStatus(),
                        examination.getStartDate() == null ? null : examination.getStartDate().toString(),
                        examination.getEndDate() == null ? null : examination.getEndDate().toString()))
                .toList());

        return dto;
    }
}
