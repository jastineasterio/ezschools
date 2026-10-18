package com.edumanage.users.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.roles.entity.Role;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.dto.CreateManagerRequest;
import com.edumanage.users.dto.CreateUserRequest;
import com.edumanage.users.dto.UserDto;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

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

    public PageResponse<UserDto> listUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public UserDto getUser(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setSchoolId(request.getSchoolId());
        user.setActive(request.isActive());
        userRepository.save(user);

        auditLogService.log("USER_CREATE", "User", user.getId(),
                "Created user " + user.getEmail(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);

        return mapToDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, CreateUserRequest request, HttpServletRequest httpRequest) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
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
        if (request.getSchoolId() != null) user.setSchoolId(request.getSchoolId());
        user.setActive(request.isActive());

        userRepository.save(user);

        auditLogService.log("USER_UPDATE", "User", user.getId(),
                "Updated user " + user.getEmail(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);

        return mapToDto(user);
    }

    @Transactional
    public void deleteUser(Long id, HttpServletRequest httpRequest) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(user);

        auditLogService.log("USER_DELETE", "User", id,
                "Deleted user " + user.getEmail(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null);
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

