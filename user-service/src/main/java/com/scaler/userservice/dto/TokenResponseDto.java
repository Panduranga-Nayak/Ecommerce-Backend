package com.scaler.userservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInSeconds;
}