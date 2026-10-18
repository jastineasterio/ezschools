package com.edumanage.config;

import com.edumanage.roles.entity.Role;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository,
                                      UserRepository userRepository,
                                      PasswordEncoder passwordEncoder) {

        return args -> initialize(roleRepository, userRepository, passwordEncoder);
    }

    @Transactional
    public void initialize(RoleRepository roleRepository,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {

        // Ensure all system roles exist.
        Role superAdminRole = getOrCreateRole(
                roleRepository,
                "SUPER_ADMIN",
                "Super Admin",
                "Platform-level administrator"
        );

        getOrCreateRole(roleRepository, "MANAGER", "Manager", "School manager");
        getOrCreateRole(roleRepository, "TEACHER", "Teacher", "School teacher");
        getOrCreateRole(roleRepository, "STUDENT", "Student", "School student");
        getOrCreateRole(roleRepository, "ACCOUNTANT", "Accountant", "School accountant");
        getOrCreateRole(roleRepository, "PARENT", "Parent/Guardian", "Parent or guardian");
        getOrCreateRole(roleRepository, "HOD", "Head of Department", "Head of department");
        getOrCreateRole(roleRepository, "LIBRARIAN", "Librarian", "School librarian");
        getOrCreateRole(roleRepository, "WARDEN", "Warden", "Boarding warden");
        getOrCreateRole(roleRepository, "TRANSPORT_OFFICER", "Transport Officer", "Transport officer");

        // Ensure the default Super Admin account exists.
        User superAdmin = userRepository.findWithRolesByEmail("admin@edumanage.com")
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail("admin@edumanage.com");
                    user.setPasswordHash(passwordEncoder.encode("Admin@123"));
                    user.setFirstName("Super");
                    user.setLastName("Admin");
                    user.setPhone("+255712345678");
                    user.setSchoolId(null);
                    user.setActive(true);
                    return userRepository.save(user);
                });

        // Ensure the Super Admin has the SUPER_ADMIN role.
        if (!superAdmin.getRoles().contains(superAdminRole)) {
            superAdmin.getRoles().add(superAdminRole);
            userRepository.save(superAdmin);
        }
    }

    private Role getOrCreateRole(RoleRepository roleRepository,
                                  String name,
                                  String displayName,
                                  String description) {

        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDisplayName(displayName);
                    role.setDescription(description);
                    role.setSystem(true);
                    role.setActive(true);
                    return roleRepository.save(role);
                });
    }
}
