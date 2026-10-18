package com.edumanage.users.service;

import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.roles.entity.Role;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findWithRolesByEmail(email)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        return CustomUserDetails.from(user, roles);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByUuid(String uuid) {
        var user = userRepository.findByUuid(UUID.fromString(uuid))
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uuid));
        var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        return CustomUserDetails.from(user, roles);
    }
}
