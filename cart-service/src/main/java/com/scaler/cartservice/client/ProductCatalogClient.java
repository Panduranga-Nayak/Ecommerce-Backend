package com.scaler.cartservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductCatalogClient {
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String productServiceBaseUrl;

    public ProductCatalogClient(RestTemplate restTemplate,
                                RetryTemplate retryTemplate,
                                @Value("${clients.product-service.base-url}") String productServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.productServiceBaseUrl = productServiceBaseUrl;
    }

    public ProductSnapshot getProduct(Long productId) {
        return retryTemplate.execute(context ->
                restTemplate.getForObject(productServiceBaseUrl + "/api/v1/products/" + productId, ProductSnapshot.class));
    }
}