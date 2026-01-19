package com.scaler.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRefreshRequestDto {
    @NotBlank
    private String refreshToken;
}