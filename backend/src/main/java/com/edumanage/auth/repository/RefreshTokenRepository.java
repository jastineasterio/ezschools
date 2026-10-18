package com.edumanage.auth.repository;

import com.edumanage.auth.entity.RefreshToken;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenAndRevokedFalseAndExpiresAtAfter(String token, java.time.LocalDateTime now);
}
