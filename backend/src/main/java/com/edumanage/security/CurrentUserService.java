package com.edumanage.security;

import com.edumanage.exception.UnauthorizedException;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the authenticated user for the current request together with the
 * roles assigned to that user. Role names are normalised so that both
 * {@code MANAGER} and {@code ROLE_MANAGER} are treated as the same authority.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        return userRepository.findWithRolesByEmail(authentication.getName())
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new UnauthorizedException("User account is no longer available"));
    }

    public Set<String> rolesOf(User user) {
        return user.getRoles().stream()
                .map(role -> role.getName() == null ? "" : role.getName().trim().toUpperCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean hasRole(User user, String role) {
        return rolesOf(user).contains(normalise(role));
    }

    public boolean hasAnyRole(User user, String... roles) {
        Set<String> assigned = rolesOf(user);
        for (String role : roles) {
            if (assigned.contains(normalise(role))) {
                return true;
            }
        }
        return false;
    }

    public String displayName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getEmail() : fullName;
    }

    private String normalise(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
