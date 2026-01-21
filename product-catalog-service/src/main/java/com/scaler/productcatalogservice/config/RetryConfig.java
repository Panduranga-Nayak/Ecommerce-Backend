package com.scaler.productcatalogservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class RetryConfig {
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(200L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(2000L);
        backOffPolicy.setSleeper(backOffPeriod -> {
            long jitter = ThreadLocalRandom.current().nextLong(50L, 150L);
            try {
                Thread.sleep(backOffPeriod + jitter);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}