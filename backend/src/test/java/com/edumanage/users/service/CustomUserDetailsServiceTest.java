package com.edumanage.users.service;

import com.edumanage.roles.entity.Role;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User activeUser() {
        Role role = new Role();
        role.setName("SUPER_ADMIN");
        role.setActive(true);

        User user = new User();
        user.setId(2L);
        user.setEmail("admin@edumanage.com");
        user.setPasswordHash("$2a$12$hashed");
        user.setSchoolId(null);
        user.setActive(true);
        user.getRoles().add(role);
        return user;
    }

    @Test
    void loadUserByUsernameReturnsDetailsWithNormalizedRoles() {
        when(userRepository.findWithRolesByEmail("admin@edumanage.com"))
                .thenReturn(Optional.of(activeUser()));

        UserDetails details = service.loadUserByUsername("admin@edumanage.com");

        assertNotNull(details);
        assertEquals("admin@edumanage.com", details.getUsername());
        assertEquals("$2a$12$hashed", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isCredentialsNonExpired());

        // Role is normalized once (ROLE_ prefix added) and never double-prefixed.
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")));
        assertFalse(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ROLE_SUPER_ADMIN")));
        assertEquals(1, details.getAuthorities().size());
    }

    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findWithRolesByEmail(anyString()))
                .thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nonexistent@edumanage.com")
        );
        assertTrue(ex.getMessage().contains("nonexistent@edumanage.com"));
    }

    @Test
    void loadUserByUsernameThrowsWhenUserIsDeleted() {
        User deletedUser = activeUser();
        deletedUser.setDeletedAt(LocalDateTime.now());

        when(userRepository.findWithRolesByEmail("admin@edumanage.com"))
                .thenReturn(Optional.of(deletedUser));

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("admin@edumanage.com")
        );
    }
}
