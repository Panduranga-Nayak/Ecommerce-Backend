package com.scaler.userservice.service;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TokenValidationResult {
    private boolean valid;
    private Long userId;
    private String email;
    private Set<String> roles;
}