package com.scaler.userservice.repo;

import com.scaler.userservice.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByRefreshTokenId(String refreshTokenId);

    Optional<UserSession> findByJwtId(String jwtId);

    List<UserSession> findByUserId(Long userId);
}