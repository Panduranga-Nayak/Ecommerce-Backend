package com.scaler.cartservice.cache;

import com.scaler.cartservice.model.Cart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CartCacheRepository {
    private final RedisTemplate<String, Cart> redisTemplate;
    private final Duration ttl;

    public CartCacheRepository(RedisTemplate<String, Cart> redisTemplate,
                               @Value("${cache.cart.ttl-seconds}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Cart getCart(Long userId) {
        return redisTemplate.opsForValue().get(buildKey(userId));
    }

    public void putCart(Long userId, Cart cart) {
        redisTemplate.opsForValue().set(buildKey(userId), cart, ttl);
    }

    public void evict(Long userId) {
        redisTemplate.delete(buildKey(userId));
    }

    private String buildKey(Long userId) {
        return "cart:" + userId;
    }
}