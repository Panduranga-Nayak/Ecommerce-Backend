package com.scaler.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenValidationRequestDto {
    @NotBlank
    private String token;
}