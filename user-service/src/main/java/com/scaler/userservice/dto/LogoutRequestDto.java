package com.scaler.userservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutRequestDto {
    private String accessToken;
    private String refreshToken;
}