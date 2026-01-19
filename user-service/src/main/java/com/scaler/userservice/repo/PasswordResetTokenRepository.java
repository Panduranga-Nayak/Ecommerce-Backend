package com.scaler.userservice.repo;

import com.scaler.userservice.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    List<PasswordResetToken> findByUserIdAndUsedAtIsNullAndExpiresAtAfter(Long userId, Instant now);
}