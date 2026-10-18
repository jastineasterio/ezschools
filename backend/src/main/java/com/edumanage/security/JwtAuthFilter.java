package com.edumanage.security;

import com.edumanage.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(7);
        var username = jwtService.extractUsername(token);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (!jwtService.isTokenExpired(token)) {
                var claims = jwtService.extractClaims(token);
                var roles = jwtService.extractRoles(token);
                var authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority(r.toUpperCase().startsWith("ROLE_") ? r.toUpperCase() : "ROLE_" + r.toUpperCase()))
                        .toList();

                var authToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                if (claims.get("schoolId") != null) {
                    TenantContext.setTenantId(claims.get("schoolId", Long.class));
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
