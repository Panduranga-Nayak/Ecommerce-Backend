package com.scaler.userservice.filter;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.userservice.model.enums.UserStatus;
import com.scaler.userservice.model.User;
import com.scaler.userservice.model.UserSession;
import com.scaler.userservice.repo.UserRepository;
import com.scaler.userservice.repo.UserSessionRepository;
import com.scaler.userservice.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private final JwtService jwtService;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    public AuthFilter(JwtService jwtService,
                      UserSessionRepository userSessionRepository,
                      UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            logger.debug("Missing or invalid Authorization header for path {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return;
        }

        String token = header.substring("Bearer ".length());
        try {
            Claims claims = jwtService.parseClaims(token);
            String jwtId = claims.getId();
            String email = claims.getSubject();

            if (jwtId == null || email == null) {
                logger.debug("Token missing jwtId or subject for path {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }

            UserSession session = userSessionRepository.findByJwtId(jwtId).orElse(null);
            if (session == null || session.getRevokedAt() != null || session.getExpiresAt().isBefore(Instant.now())) {
                logger.debug("Session invalid for jwtId {} path {}", jwtId, request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
                return;
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                logger.debug("User not active for email {} path {}", email, request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not active");
                return;
            }

            Set<String> roles = extractRoles(claims, user);

            AuthenticatedUser authenticatedUser = new AuthenticatedUser();
            authenticatedUser.setUserId(user.getId());
            authenticatedUser.setEmail(user.getEmail());
            authenticatedUser.setRoles(roles);

            RequestContext.setCurrentUser(authenticatedUser);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getEmail(), null,
                    roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toSet()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            logger.debug("Token parse failed for path {}: {}", request.getRequestURI(), e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        } finally {
            RequestContext.clear();
        }
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

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }

        if (path.startsWith("/actuator")) {
            return true;
        }

        return path.startsWith("/api/v1/users/register")
                || path.startsWith("/api/v1/users/login")
                || path.startsWith("/api/v1/users/password/reset")
                || path.startsWith("/api/v1/users/tokens/validate")
                || path.startsWith("/api/v1/users/token/refresh");
    }
}