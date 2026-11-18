package com.edumanage.audit.service;

import com.edumanage.security.CustomUserDetails;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorResolver {

    private final UserRepository userRepository;

    public ActorResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Actor currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Actor.system();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            String role = details.authorities().stream()
                    .map(Object::toString)
                    .findFirst()
                    .orElse(null);
            User user = userRepository.findById(details.id()).orElse(null);
            String name = displayName(user, details.email());
            return new Actor(details.id(), name, role, details.schoolId());
        }
        return new Actor(null, auth.getName(), null, null);
    }

    private String displayName(User user, String email) {
        if (user != null) {
            String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
            String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
            String fullName = (firstName + " " + lastName).trim();
            if (!fullName.isEmpty()) {
                return fullName;
            }
        }
        return email != null ? email : "System";
    }

    public record Actor(Long userId, String name, String role, Long schoolId) {
        public static Actor system() {
            return new Actor(null, "System", null, null);
        }

        public boolean isAuthenticated() {
            return userId != null;
        }
    }
}
