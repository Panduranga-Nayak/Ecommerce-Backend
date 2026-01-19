package com.scaler.userservice.service;

import com.scaler.userservice.dto.*;
import com.scaler.userservice.model.User;

public interface UserService {
    User registerLocal(UserRegistrationRequestDto request);

    TokenPair login(LoginRequestDto request);

    TokenPair refresh(TokenRefreshRequestDto request);

    void logout(LogoutRequestDto request);

    User getProfile(Long userId);

    User updateProfile(Long userId, UpdateProfileRequestDto request);

    void requestPasswordReset(PasswordResetRequestDto request, String resetBaseUrl);

    void confirmPasswordReset(PasswordResetConfirmRequestDto request);

    TokenValidationResult validateAccessToken(String token);
}
