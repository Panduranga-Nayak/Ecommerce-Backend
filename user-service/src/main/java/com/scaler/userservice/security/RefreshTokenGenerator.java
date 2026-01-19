package com.scaler.userservice.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Component
public class RefreshTokenGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenDetails generate() {
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);

        String tokenId = UUID.randomUUID().toString().replace("-", "");
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        String token = tokenId + "." + secret;

        RefreshTokenDetails details = new RefreshTokenDetails();
        details.setTokenId(tokenId);
        details.setSecret(secret);
        details.setToken(token);
        return details;
    }

    public RefreshTokenDetails parse(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return null;
        }

        RefreshTokenDetails details = new RefreshTokenDetails();
        details.setTokenId(parts[0]);
        details.setSecret(parts[1]);
        details.setToken(token);
        return details;
    }
}