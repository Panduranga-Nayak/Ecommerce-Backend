package com.scaler.userservice.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenPair {
    private String accessToken;
    private String refreshToken;
    private long expiresInSeconds;
    private String tokenType;
}