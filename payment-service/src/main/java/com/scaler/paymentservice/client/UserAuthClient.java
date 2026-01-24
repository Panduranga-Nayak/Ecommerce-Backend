package com.scaler.paymentservice.client;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserAuthClient {
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String userServiceBaseUrl;
    private final String internalSecret;

    public UserAuthClient(RestTemplate restTemplate,
                          RetryTemplate retryTemplate,
                          @Value("${clients.user-service.base-url}") String userServiceBaseUrl,
                          @Value("${security.internal.secret:}") String internalSecret) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.userServiceBaseUrl = userServiceBaseUrl;
        this.internalSecret = internalSecret;
    }

    public AuthenticatedUser validate(String token) {
        return retryTemplate.execute(context -> {
            TokenValidationRequest request = new TokenValidationRequest();
            request.setToken(token);

            HttpHeaders headers = new HttpHeaders();
            if (internalSecret != null && !internalSecret.isBlank()) {
                headers.set("X-Internal-Secret", internalSecret);
            }

            HttpEntity<TokenValidationRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<TokenValidationResponse> response =
                    restTemplate.postForEntity(userServiceBaseUrl + "/api/v1/users/tokens/validate",
                            entity, TokenValidationResponse.class);

            TokenValidationResponse body = response.getBody();
            if (body == null || !body.isValid()) {
                return null;
            }

            AuthenticatedUser user = new AuthenticatedUser();
            user.setUserId(body.getUserId());
            user.setEmail(body.getEmail());
            user.setRoles(body.getRoles());
            return user;
        });
    }
}