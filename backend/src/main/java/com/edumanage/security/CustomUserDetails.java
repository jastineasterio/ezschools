package com.edumanage.security;

import com.edumanage.users.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(
        Long id,
        String email,
        String password,
        Long schoolId,
        boolean isActive,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    public static CustomUserDetails from(User user, List<String> roles) {
        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.toUpperCase().startsWith("ROLE_") ? r.toUpperCase() : "ROLE_" + r.toUpperCase()))
                .toList();
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getSchoolId(),
                user.isActive(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
