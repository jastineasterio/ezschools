package com.edumanage.notifications.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.parents.entity.Parent;
import com.edumanage.parents.repository.ParentRepository;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class CommunicationRecipientService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final List<String> STAFF_ROLES = List.of(
            "MANAGER", "TEACHER", "ACCOUNTANT", "HOD", "LIBRARIAN", "WARDEN", "TRANSPORT_OFFICER");

    private final StudentProfileRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherProfileRepository teacherRepository;
    private final UserRepository userRepository;

    public CommunicationRecipientService(StudentProfileRepository studentRepository,
                                         ParentRepository parentRepository,
                                         TeacherProfileRepository teacherRepository,
                                         UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommunicationRecipient> resolve(Long schoolId, String group, List<Long> selectedUserIds) {
        Map<String, CommunicationRecipient> unique = new LinkedHashMap<>();
        switch (group) {
            case "STUDENTS" -> studentRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, Pageable.unpaged())
                    .getContent().forEach(profile -> addStudent(unique, profile));
            case "PARENTS" -> parentRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, Pageable.unpaged())
                    .getContent().forEach(parent -> addParent(unique, parent));
            case "TEACHERS" -> teacherRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, Pageable.unpaged())
                    .getContent().forEach(profile -> addTeacher(unique, profile));
            case "STAFF" -> userRepository.findActiveBySchoolAndRoleNames(schoolId, STAFF_ROLES)
                    .forEach(user -> addUser(unique, user));
            case "ALL_USERS" -> {
                studentRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, Pageable.unpaged())
                        .getContent().forEach(profile -> addStudent(unique, profile));
                parentRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, Pageable.unpaged())
                        .getContent().forEach(parent -> addParent(unique, parent));
                teacherRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, Pageable.unpaged())
                        .getContent().forEach(profile -> addTeacher(unique, profile));
                userRepository.findActiveBySchoolAndRoleNames(schoolId, STAFF_ROLES)
                        .forEach(user -> addUser(unique, user));
            }
            case "SELECTED" -> addSelected(unique, schoolId, selectedUserIds);
            default -> throw new BadRequestException("Unsupported recipient group");
        }
        return List.copyOf(unique.values());
    }

    private void addStudent(Map<String, CommunicationRecipient> recipients, StudentProfile profile) {
        User user = profile.getUser();
        if (user != null && (!user.isActive() || user.isDeleted())) {
            return;
        }
        String email = firstNonBlank(profile.getEmail(), user == null ? null : user.getEmail());
        String phone = firstNonBlank(profile.getPhone(), user == null ? null : user.getPhone());
        add(recipients, new CommunicationRecipient("STUDENT", profile.getId(),
                join(profile.getFirstName(), profile.getLastName()), email, phone));
    }

    private void addParent(Map<String, CommunicationRecipient> recipients, Parent parent) {
        if (parent.isDeleted()) {
            return;
        }
        add(recipients, new CommunicationRecipient("PARENT", parent.getId(),
                join(parent.getFirstName(), parent.getLastName()), parent.getEmail(), parent.getPhone()));
    }

    private void addTeacher(Map<String, CommunicationRecipient> recipients, TeacherProfile profile) {
        User user = profile.getUser();
        if (profile.isDeleted() || user == null || !user.isActive() || user.isDeleted()) {
            return;
        }
        addUser(recipients, user);
    }

    private void addSelected(Map<String, CommunicationRecipient> recipients, Long schoolId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Select at least one recipient");
        }
        List<User> users = userRepository.findAllById(ids);
        if (users.size() != ids.stream().distinct().count()
                || users.stream().anyMatch(user -> !schoolId.equals(user.getSchoolId())
                || !user.isActive() || user.isDeleted())) {
            throw new ForbiddenException("One or more selected recipients are not eligible for this school");
        }
        users.forEach(user -> addUser(recipients, user));
    }

    private void addUser(Map<String, CommunicationRecipient> recipients, User user) {
        add(recipients, new CommunicationRecipient("USER", user.getId(),
                join(user.getFirstName(), user.getLastName()), user.getEmail(), user.getPhone()));
    }

    private void add(Map<String, CommunicationRecipient> recipients, CommunicationRecipient recipient) {
        recipients.putIfAbsent(recipient.type() + ":" + recipient.id(), recipient);
    }

    public boolean hasEligibleContact(CommunicationRecipient recipient, String channel) {
        return switch (channel) {
            case "SMS" -> {
                try {
                    DarsmsProviderService.normalizeTanzanianPhone(recipient.phone());
                    yield true;
                } catch (NotificationProviderException ignored) {
                    yield false;
                }
            }
            case "EMAIL" -> recipient.email() != null
                    && EMAIL.matcher(recipient.email().trim()).matches();
            default -> throw new BadRequestException("Unsupported channel");
        };
    }

    public static String currentUserEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("User not authenticated");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.email();
        }
        return authentication.getName();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String join(String first, String last) {
        return String.join(" ", List.of(first == null ? "" : first, last == null ? "" : last)).trim();
    }
}
