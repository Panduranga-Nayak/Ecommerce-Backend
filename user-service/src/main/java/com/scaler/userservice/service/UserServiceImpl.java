package com.scaler.userservice.service;

import com.scaler.ecommerce.common.events.PasswordResetRequestedEvent;
import com.scaler.ecommerce.common.events.UserRegisteredEvent;
import com.scaler.userservice.dto.*;
import com.scaler.userservice.exception.ForbiddenException;
import com.scaler.userservice.exception.InvalidCredentialsException;
import com.scaler.userservice.exception.InvalidTokenException;
import com.scaler.userservice.exception.UserAlreadyExistsException;
import com.scaler.userservice.exception.UserNotFoundException;
import com.scaler.userservice.model.enums.AuthProvider;
import com.scaler.userservice.model.enums.UserRole;
import com.scaler.userservice.model.enums.UserStatus;
import com.scaler.userservice.model.PasswordResetToken;
import com.scaler.userservice.model.User;
import com.scaler.userservice.model.UserSession;
import com.scaler.userservice.kafka.KafkaEventPublisher;
import com.scaler.userservice.repo.PasswordResetTokenRepository;
import com.scaler.userservice.repo.UserRepository;
import com.scaler.userservice.repo.UserSessionRepository;
import com.scaler.userservice.security.JwtService;
import com.scaler.userservice.security.RefreshTokenDetails;
import com.scaler.userservice.security.RefreshTokenGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final long refreshTokenTtlSeconds;
    private final long accessTokenTtlSeconds;
    private final long passwordResetTtlSeconds;
    private final String passwordResetBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserServiceImpl(UserRepository userRepository,
                           UserSessionRepository userSessionRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           RefreshTokenGenerator refreshTokenGenerator,
                           KafkaEventPublisher kafkaEventPublisher,
                           @Value("${security.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds,
                           @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds,
                           @Value("${security.password-reset.token-ttl-seconds}") long passwordResetTtlSeconds,
                           @Value("${app.password-reset.base-url}") String passwordResetBaseUrl) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        this.passwordResetTtlSeconds = passwordResetTtlSeconds;
        this.passwordResetBaseUrl = passwordResetBaseUrl;
    }

    @Override
    @Transactional
    public User registerLocal(UserRegistrationRequestDto request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            throw new UserAlreadyExistsException("User with email already exists");
        });

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProvider(AuthProvider.LOCAL);
        user.setProviderUserId(null);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(UserRole.CUSTOMER));

        User saved = userRepository.save(user);
        publishUserRegistered(saved, "LOCAL");
        return saved;
    }


    @Override
    @Transactional
    public TokenPair login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new InvalidCredentialsException("Password login is not enabled for this account");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return issueTokenPair(user, null);
    }


    @Override
    @Transactional
    public TokenPair refresh(TokenRefreshRequestDto request) {
        RefreshTokenDetails details = refreshTokenGenerator.parse(request.getRefreshToken());
        if (details == null) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        UserSession session = userSessionRepository.findByRefreshTokenId(details.getTokenId())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        if (!passwordEncoder.matches(details.getSecret(), session.getRefreshTokenHash())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        return issueTokenPair(session.getUser(), session);
    }

    @Override
    @Transactional
    public void logout(LogoutRequestDto request) {
        UserSession session = null;
        if (request.getRefreshToken() != null) {
            RefreshTokenDetails details = refreshTokenGenerator.parse(request.getRefreshToken());
            if (details != null) {
                session = userSessionRepository.findByRefreshTokenId(details.getTokenId()).orElse(null);
            }
        }

        if (session == null && request.getAccessToken() != null) {
            try {
                Claims claims = jwtService.parseClaims(request.getAccessToken());
                session = userSessionRepository.findByJwtId(claims.getId()).orElse(null);
            } catch (JwtException ignored) {
                session = null;
            }
        }

        if (session != null) {
            session.setRevokedAt(Instant.now());
            userSessionRepository.save(session);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setName(request.getName());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto request, String resetBaseUrl) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return;
        }

        List<PasswordResetToken> activeTokens =
                passwordResetTokenRepository.findByUserIdAndUsedAtIsNullAndExpiresAtAfter(user.getId(), Instant.now());
        for (PasswordResetToken token : activeTokens) {
            token.setUsedAt(Instant.now());
        }

        String rawToken = generateResetToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(passwordEncoder.encode(rawToken));
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiresAt(Instant.now().plusSeconds(passwordResetTtlSeconds));

        passwordResetTokenRepository.save(resetToken);

        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent();
        event.setUserId(user.getId());
        event.setEmail(user.getEmail());
        event.setResetToken(rawToken);
        event.setResetUrl(buildResetUrl(resetBaseUrl != null ? resetBaseUrl : passwordResetBaseUrl, rawToken));

        kafkaEventPublisher.publish("user.password_reset.requested", getCorrelationId(), event);
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<PasswordResetToken> candidates =
                passwordResetTokenRepository.findByUserIdAndUsedAtIsNullAndExpiresAtAfter(user.getId(), Instant.now());

        PasswordResetToken matched = null;
        for (PasswordResetToken token : candidates) {
            if (passwordEncoder.matches(request.getToken(), token.getTokenHash())) {
                matched = token;
                break;
            }
        }

        if (matched == null) {
            throw new InvalidTokenException("Invalid or expired reset token");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        matched.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(matched);

        revokeAllSessions(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenValidationResult validateAccessToken(String token) {
        TokenValidationResult result = new TokenValidationResult();
        try {
            Claims claims = jwtService.parseClaims(token);
            String jwtId = claims.getId();
            String email = claims.getSubject();

            if (jwtId == null || email == null) {
                logger.debug("Token validation failed: missing jwtId or subject");
                result.setValid(false);
                return result;
            }

            UserSession session = userSessionRepository.findByJwtId(jwtId).orElse(null);
            if (session == null || session.getRevokedAt() != null || session.getExpiresAt().isBefore(Instant.now())) {
                logger.debug("Token validation failed: session invalid for jwtId {}", jwtId);
                result.setValid(false);
                return result;
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                logger.debug("Token validation failed: user not active for email {}", email);
                result.setValid(false);
                return result;
            }

            Set<String> roles = extractRoles(claims, user);

            result.setValid(true);
            result.setUserId(user.getId());
            result.setEmail(user.getEmail());
            result.setRoles(roles);
            return result;
        } catch (JwtException e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            result.setValid(false);
            return result;
        }
    }

    private TokenPair issueTokenPair(User user, UserSession existingSession) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("User is disabled");
        }

        String jwtId = UUID.randomUUID().toString().replace("-", "");
        String accessToken = jwtService.generateAccessToken(user, jwtId);

        RefreshTokenDetails refreshTokenDetails = refreshTokenGenerator.generate();
        Instant now = Instant.now();
        Instant refreshExpiry = now.plusSeconds(refreshTokenTtlSeconds);

        UserSession session = existingSession != null ? existingSession : new UserSession();
        session.setUser(user);
        session.setJwtId(jwtId);
        session.setRefreshTokenId(refreshTokenDetails.getTokenId());
        session.setRefreshTokenHash(passwordEncoder.encode(refreshTokenDetails.getSecret()));
        session.setIssuedAt(now);
        session.setExpiresAt(refreshExpiry);
        session.setLastUsedAt(now);
        session.setRevokedAt(null);

        userSessionRepository.save(session);

        TokenPair tokenPair = new TokenPair();
        tokenPair.setAccessToken(accessToken);
        tokenPair.setRefreshToken(refreshTokenDetails.getToken());
        tokenPair.setExpiresInSeconds(accessTokenTtlSeconds);
        tokenPair.setTokenType("Bearer");
        return tokenPair;
    }

    private Set<String> extractRoles(Claims claims, User user) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Collection<?> rolesCollection) {
            return rolesCollection.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }

        return user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
    }

    private void revokeAllSessions(Long userId) {
        List<UserSession> sessions = userSessionRepository.findByUserId(userId);
        Instant now = Instant.now();
        for (UserSession session : sessions) {
            session.setRevokedAt(now);
        }
        userSessionRepository.saveAll(sessions);
    }

    private void publishUserRegistered(User user, String method) {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(user.getId());
        event.setEmail(user.getEmail());
        event.setName(user.getName());
        event.setRegistrationMethod(method);

        kafkaEventPublisher.publish("user.registered", getCorrelationId(), event);
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildResetUrl(String baseUrl, String token) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return token;
        }
        if (baseUrl.contains("?")) {
            return baseUrl + "&token=" + token;
        }
        return baseUrl + "?token=" + token;
    }
}
