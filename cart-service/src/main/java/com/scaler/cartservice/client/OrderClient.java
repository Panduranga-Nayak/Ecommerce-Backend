package com.scaler.cartservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderClient {
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String orderServiceBaseUrl;

    public OrderClient(RestTemplate restTemplate,
                       RetryTemplate retryTemplate,
                       @Value("${clients.order-service.base-url}") String orderServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.orderServiceBaseUrl = orderServiceBaseUrl;
    }

    public OrderCreateResponse createOrder(OrderCreateRequest request, String idempotencyKey, String accessToken) {
        return retryTemplate.execute(context -> {
            HttpHeaders headers = new HttpHeaders();
            if (idempotencyKey != null) {
                headers.set("Idempotency-Key", idempotencyKey);
            }
            if (accessToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }

            HttpEntity<OrderCreateRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<OrderCreateResponse> response = restTemplate.postForEntity(
                    orderServiceBaseUrl + "/api/v1/orders", entity, OrderCreateResponse.class);
            return response.getBody();
        });
    }
}