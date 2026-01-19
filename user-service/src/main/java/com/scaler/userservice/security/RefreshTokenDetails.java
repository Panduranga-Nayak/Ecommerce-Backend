package com.scaler.userservice.security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenDetails {
    private String tokenId;
    private String token;
    private String secret;
}