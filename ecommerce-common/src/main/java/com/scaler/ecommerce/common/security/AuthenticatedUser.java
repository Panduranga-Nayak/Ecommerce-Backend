package com.scaler.ecommerce.common.security;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AuthenticatedUser {
    private Long userId;
    private String email;
    private Set<String> roles;
}
