package com.edumanage.users.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.parents.entity.Parent;
import com.edumanage.parents.repository.ParentRepository;
import com.edumanage.roles.entity.Role;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.dto.CreateManagerRequest;
import com.edumanage.users.dto.CreateSchoolUserRequest;
import com.edumanage.users.dto.CreateUserRequest;
import com.edumanage.users.dto.UserDto;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final ParentRepository parentRepository;

    private static final Set<String> SCHOOL_USER_ROLES = Set.of(
            "TEACHER", "STUDENT", "ACCOUNTANT", "PARENT", "HOD",
            "LIBRARIAN", "WARDEN", "TRANSPORT_OFFICER");
    private static final Set<String> SCHOOL_LIST_ROLES = Set.of(
            "MANAGER", "TEACHER", "STUDENT", "ACCOUNTANT", "PARENT", "HOD",
            "LIBRARIAN", "WARDEN", "TRANSPORT_OFFICER");

    @Transactional
    public UserDto createManager(CreateManagerRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        var school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        if (!school.isActive()) {
            throw new BadRequestException("School is not active");
        }

        var managerRole = roleRepository.findByName("MANAGER")
                .orElseThrow(() -> new BadRequestException("MANAGER role not found"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setSchoolId(school.getId());
        user.setActive(request.isActive());
        user.getRoles().add(managerRole);
        school.getUsers().add(user);

        userRepository.save(user);

        auditLogService.log("MANAGER_CREATE", "User", user.getId(),
                "Created manager " + user.getEmail() + " for school " + school.getName(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);

        return mapToDto(user);
    }

    @Transactional(readOnly = true)
public PageResponse<UserDto> listAllUsers(Pageable pageable) {
    Page<User> page = userRepository.findAllWithRolesBy(pageable);
    return PageResponse.from(page.map(this::mapToDto));
}

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(Pageable pageable, String search, String role, Boolean active) {
        Long schoolId = requireManagerSchoolId();
        String normalizedRole = role == null || role.isBlank()
                ? null : role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole != null && !SCHOOL_LIST_ROLES.contains(normalizedRole)) {
            throw new BadRequestException("Unsupported school user role filter");
        }
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<User> page = userRepository.findSchoolUsers(
                schoolId, normalizedSearch, normalizedRole, active, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        var user = findManagedUser(id, requireManagerSchoolId());
        return mapToDto(user);
    }

    @Transactional
    public UserDto createSchoolUser(CreateSchoolUserRequest request, HttpServletRequest httpRequest) {
        Long schoolId = requireManagerSchoolId();
        String roleName = request.getRole().trim().toUpperCase(Locale.ROOT);
        if (!SCHOOL_USER_ROLES.contains(roleName)) {
            throw new ForbiddenException("Managers cannot assign this role");
        }

        String email = request.getEmail().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive() || school.isSuspended()) {
            throw new BadRequestException("School is not active");
        }
        Role role = roleRepository.findByName(roleName)
                .filter(Role::isActive)
                .orElseThrow(() -> new BadRequestException("Role is not available"));
        validateRoleProfile(schoolId, roleName, request);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setSchoolId(schoolId);
        user.setActive(true);
        user.getRoles().add(role);
        school.getUsers().add(user);
        userRepository.save(user);

        createRoleProfile(user, schoolId, roleName, request);
        auditLogService.log("USER_CREATE", "User", user.getId(),
                "Created " + roleName + " user " + user.getEmail() + " for school " + school.getName(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);

        return mapToDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, CreateUserRequest request, HttpServletRequest httpRequest) {
        Long schoolId = requireManagerSchoolId();
        var user = findManagedUser(id, schoolId);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new ConflictException("Email already registered");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getSchoolId() != null && !Objects.equals(request.getSchoolId(), schoolId)) {
            throw new ForbiddenException("Managers cannot move users between schools");
        }
        user.setActive(request.isActive());

        userRepository.save(user);

        auditLogService.log("USER_UPDATE", "User", user.getId(),
                "Updated user " + user.getEmail(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);

        return mapToDto(user);
    }

    @Transactional
    public void deleteUser(Long id, HttpServletRequest httpRequest) {
        var user = findManagedUser(id, requireManagerSchoolId());

        userRepository.delete(user);

        auditLogService.log("USER_DELETE", "User", id,
                "Deleted user " + user.getEmail(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);
    }

    private void createRoleProfile(User user, Long schoolId, String roleName, CreateSchoolUserRequest request) {
        switch (roleName) {
            case "STUDENT" -> {
                StudentProfile profile = new StudentProfile();
                profile.setUserId(user.getId());
                profile.setSchoolId(schoolId);
                profile.setAdmissionNumber(request.getAdmissionNumber().trim());
                profile.setFirstName(user.getFirstName());
                profile.setLastName(user.getLastName());
                profile.setGender(request.getGender());
                profile.setDateOfBirth(request.getDateOfBirth());
                profile.setRegistrationDate(request.getRegistrationDate() == null
                        ? LocalDate.now() : request.getRegistrationDate());
                profile.setPhone(user.getPhone());
                profile.setEmail(user.getEmail());
                profile.setAddress(request.getAddress());
                profile.setStudentType(request.getStudentType() == null || request.getStudentType().isBlank()
                        ? "DAY" : request.getStudentType());
                profile.setEmergencyContactName(request.getEmergencyContactName());
                profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
                studentProfileRepository.save(profile);
            }
            case "TEACHER" -> {
                TeacherProfile profile = new TeacherProfile();
                profile.setUserId(user.getId());
                profile.setSchoolId(schoolId);
                profile.setGender(request.getGender().toUpperCase(Locale.ROOT));
                profile.setDateOfBirth(request.getDateOfBirth());
                profile.setEmploymentStatus(request.getEmploymentStatus().toUpperCase(Locale.ROOT));
                profile.setQualification(request.getQualification().trim());
                profile.setExperienceYears(request.getExperienceYears());
                teacherProfileRepository.save(profile);
            }
            case "PARENT" -> {
                Parent profile = new Parent();
                profile.setSchoolId(schoolId);
                profile.setFirstName(user.getFirstName());
                profile.setLastName(user.getLastName());
                profile.setEmail(user.getEmail());
                profile.setPhone(user.getPhone());
                profile.setGender(request.getGender());
                profile.setOccupation(request.getOccupation());
                profile.setAddress(request.getAddress());
                profile.setUserId(user.getId());
                parentRepository.save(profile);
            }
            default -> {
            }
        }
    }

    private void validateRoleProfile(Long schoolId, String roleName, CreateSchoolUserRequest request) {
        switch (roleName) {
            case "STUDENT" -> {
                if (request.getAdmissionNumber() == null || request.getAdmissionNumber().isBlank()) {
                    throw new BadRequestException("Admission number is required for students");
                }
                if (studentProfileRepository.existsBySchoolIdAndAdmissionNumber(
                        schoolId, request.getAdmissionNumber().trim())) {
                    throw new ConflictException("Admission number already exists in this school");
                }
            }
            case "TEACHER" -> {
                if (request.getGender() == null || request.getGender().isBlank()
                        || request.getDateOfBirth() == null
                        || request.getEmploymentStatus() == null || request.getEmploymentStatus().isBlank()
                        || request.getQualification() == null || request.getQualification().isBlank()
                        || request.getExperienceYears() == null || request.getExperienceYears() < 0
                        || request.getExperienceYears() > 100) {
                    throw new BadRequestException("Teacher gender, date of birth, employment status, qualification, and valid experience years are required");
                }
                if (!Set.of("MALE", "FEMALE", "OTHER")
                        .contains(request.getGender().toUpperCase(Locale.ROOT))) {
                    throw new BadRequestException("Teacher gender is invalid");
                }
                if (!Set.of("ACTIVE", "ON_LEAVE", "TERMINATED", "RETIRED")
                        .contains(request.getEmploymentStatus().toUpperCase(Locale.ROOT))) {
                    throw new BadRequestException("Teacher employment status is invalid");
                }
            }
            case "PARENT" -> {
                if (request.getPhone() == null || request.getPhone().isBlank()) {
                    throw new BadRequestException("Phone number is required for parents");
                }
            }
            default -> {
            }
        }
    }

    private User findManagedUser(Long id, Long schoolId) {
        User user = userRepository.findByIdAndSchoolIdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRoles().stream().anyMatch(role -> "SUPER_ADMIN".equalsIgnoreCase(role.getName()))) {
            throw new ResourceNotFoundException("User not found");
        }
        return user;
    }

    private Long requireManagerSchoolId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new ForbiddenException("A school manager identity is required");
        }

        User manager = userRepository.findWithRolesByEmail(authentication.getName())
                .filter(user -> !user.isDeleted() && user.isActive())
                .orElseThrow(() -> new ForbiddenException("A school manager identity is required"));
        boolean isManager = manager.getRoles().stream()
                .anyMatch(role -> "MANAGER".equalsIgnoreCase(role.getName()));
        Long schoolId = manager.getSchoolId();
        if (!isManager || schoolId == null || !Objects.equals(TenantContext.getTenantId(), schoolId)) {
            throw new ForbiddenException("A valid school manager assignment is required");
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive() || school.isSuspended()) {
            throw new ForbiddenException("The assigned school is not active");
        }
        return schoolId;
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUuid(user.getUuid() != null ? user.getUuid().toString() : null);
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setSchoolId(user.getSchoolId());
        dto.setSchoolName(user.getSchoolId() == null ? null : schoolRepository.findById(user.getSchoolId()).map(School::getName).orElse(null));
        dto.setActive(user.isActive());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRoles(
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toList())
        );
        return dto;
    }
}
