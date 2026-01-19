package com.scaler.userservice.controller;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.userservice.dto.*;
import com.scaler.userservice.mapper.UserMapper;
import com.scaler.userservice.service.TokenPair;
import com.scaler.userservice.service.TokenValidationResult;
import com.scaler.userservice.service.UserService;
import com.scaler.userservice.model.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final String internalSecret;

    public UserController(UserService userService,
                          @Value("${security.internal.secret:}") String internalSecret) {
        this.userService = userService;
        this.internalSecret = internalSecret;
    }

    @PostMapping("/register")
    public UserProfileResponseDto register(@Valid @RequestBody UserRegistrationRequestDto request) {
        User user = userService.registerLocal(request);

        return UserMapper.toProfile(user);
    }


    @PostMapping("/login")
    public TokenResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        TokenPair tokenPair = userService.login(request);

        return UserMapper.toTokenResponse(tokenPair);
    }


    @PostMapping("/token/refresh")
    public TokenResponseDto refresh(@Valid @RequestBody TokenRefreshRequestDto request) {
        TokenPair tokenPair = userService.refresh(request);

        return UserMapper.toTokenResponse(tokenPair);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequestDto request) {
        userService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserProfileResponseDto getProfile() {
        AuthenticatedUser currentUser = RequestContext.getCurrentUser();
        User user = userService.getProfile(currentUser.getUserId());
        return UserMapper.toProfile(user);
    }

    @PutMapping("/me")
    public UserProfileResponseDto updateProfile(@Valid @RequestBody UpdateProfileRequestDto request) {
        AuthenticatedUser currentUser = RequestContext.getCurrentUser();
        User user = userService.updateProfile(currentUser.getUserId(), request);
        return UserMapper.toProfile(user);
    }

    @PostMapping("/password/reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        userService.requestPasswordReset(request, request.getResetBaseUrl());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequestDto request) {
        userService.confirmPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tokens/validate")
    public TokenValidationResponseDto validateToken(@Valid @RequestBody TokenValidationRequestDto request,
                                                    @RequestHeader(value = "X-Internal-Secret", required = false) String secretHeader) {
        if (internalSecret != null && !internalSecret.isBlank()) {
            if (secretHeader == null || !internalSecret.equals(secretHeader)) {
                throw new com.scaler.userservice.exception.ForbiddenException("Invalid internal secret");
            }
        }

        TokenValidationResult result = userService.validateAccessToken(request.getToken());
        TokenValidationResponseDto response = new TokenValidationResponseDto();
        response.setValid(result.isValid());
        response.setUserId(result.getUserId());
        response.setEmail(result.getEmail());
        response.setRoles(result.getRoles());
        return response;
    }
}
