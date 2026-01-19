package com.scaler.userservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TokenValidationResponseDto {
    private boolean valid;
    private Long userId;
    private String email;
    private Set<String> roles;
}