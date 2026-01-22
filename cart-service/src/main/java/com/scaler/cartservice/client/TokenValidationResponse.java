package com.scaler.cartservice.client;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TokenValidationResponse {
    private boolean valid;
    private Long userId;
    private String email;
    private Set<String> roles;
}