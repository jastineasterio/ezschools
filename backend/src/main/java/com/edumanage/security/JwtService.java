package com.edumanage.security;

import com.edumanage.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-validity-ms}")
    private long accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity-ms}")
    private long refreshTokenValidity;

    public String generateAccessToken(UserDetails userDetails, Long userId, Long schoolId, List<String> roles) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userId)
                .claim("schoolId", schoolId)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    public Long extractSchoolId(String token) {
        return extractClaims(token).get("schoolId", Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        var claims = extractClaims(token);
        var roles = (List<String>) claims.get("roles", List.class);
        return roles.stream().map(r -> r.toString()).collect(Collectors.toList());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        var username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    private SecretKey getSigningKey() {
        var keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
