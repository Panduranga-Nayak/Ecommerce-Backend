package com.scaler.productcatalogservice.filter;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.productcatalogservice.client.UserAuthClient;
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

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final UserAuthClient userAuthClient;

    public AuthFilter(UserAuthClient userAuthClient) {
        this.userAuthClient = userAuthClient;
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
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return;
        }

        String token = header.substring("Bearer ".length());
        AuthenticatedUser user = userAuthClient.validate(token);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        RequestContext.setCurrentUser(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toSet()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }

        if (path.startsWith("/actuator")) {
            return true;
        }

        if (path.startsWith("/api/v1/products/search")) {
            return true;
        }

        if (path.startsWith("/api/v1/products") && "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (path.startsWith("/api/v1/categories") && "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        return false;
    }
}