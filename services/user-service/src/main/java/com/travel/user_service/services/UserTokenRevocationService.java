package com.travel.user_service.services;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserTokenRevocationService {

    private static final String USER_BLACKLIST_KEY_PREFIX = "blacklist:user:";

    private final StringRedisTemplate redisTemplate;
    private final Duration tokenLifetime;

    public UserTokenRevocationService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.expiration}") long tokenExpirationMillis) {
        if (tokenExpirationMillis <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero");
        }
        this.redisTemplate = redisTemplate;
        this.tokenLifetime = Duration.ofMillis(tokenExpirationMillis);
    }

    public void revokeUserTokens(Long userId) {
        redisTemplate.opsForValue().set(USER_BLACKLIST_KEY_PREFIX + userId, "revoked", tokenLifetime);
    }
}
