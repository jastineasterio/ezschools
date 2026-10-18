package com.edumanage.security;

import com.edumanage.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(
                1L,
                "test@example.com",
                "password",
                2L,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
    }

    @Test
    void testGetAuthorities() {
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }

    @Test
    void testGetPassword() {
        assertEquals("password", userDetails.getPassword());
    }

    @Test
    void testGetUsername() {
        assertEquals("test@example.com", userDetails.getUsername());
    }

    @Test
    void testIsEnabled() {
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void testFromNormalizesRolesWithoutDoublePrefix() {
        User user = new User();
        user.setId(1L);
        user.setEmail("a@b.com");
        user.setPasswordHash("p");
        user.setSchoolId(null);
        user.setActive(true);

        var prefixed = CustomUserDetails.from(
                user,
                List.of("ROLE_ADMIN", "SUPER_ADMIN", "manager")
        );
        assertEquals(3, prefixed.getAuthorities().size());
        assertTrue(prefixed.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(prefixed.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        assertTrue(prefixed.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER")));
        assertFalse(prefixed.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_ROLE_")));
    }

    @Test
    void testInactiveUserIsDisabledAndLocked() {
        var inactive = new CustomUserDetails(
                1L, "inactive@b.com", "p", null, false,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        assertFalse(inactive.isEnabled());
        assertFalse(inactive.isAccountNonLocked());
        assertTrue(inactive.isAccountNonExpired());
        assertTrue(inactive.isCredentialsNonExpired());
    }
}
