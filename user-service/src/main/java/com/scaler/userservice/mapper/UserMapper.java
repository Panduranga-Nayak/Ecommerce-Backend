package com.scaler.userservice.mapper;

import com.scaler.userservice.dto.TokenResponseDto;
import com.scaler.userservice.dto.UserProfileResponseDto;
import com.scaler.userservice.service.TokenPair;
import com.scaler.userservice.model.User;

import java.util.stream.Collectors;

public class UserMapper {
    public static UserProfileResponseDto toProfile(User user) {
        if (user == null) {
            return null;
        }

        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRoles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
        return dto;
    }

    public static TokenResponseDto toTokenResponse(TokenPair tokenPair) {
        TokenResponseDto dto = new TokenResponseDto();
        dto.setAccessToken(tokenPair.getAccessToken());
        dto.setRefreshToken(tokenPair.getRefreshToken());
        dto.setTokenType(tokenPair.getTokenType());
        dto.setExpiresInSeconds(tokenPair.getExpiresInSeconds());
        return dto;
    }
}