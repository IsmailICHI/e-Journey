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
        String key = USER_BLACKLIST_KEY_PREFIX + userId;
        Long currentVersion = readVersion(key);
        if (currentVersion == null) {
            redisTemplate.opsForValue().set(key, "0");
        } else if (currentVersion < 0) {
            redisTemplate.opsForValue().set(key, "1", tokenLifetime);
            return;
        }
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, tokenLifetime);
    }

    public long currentTokenVersion(Long userId) {
        String key = USER_BLACKLIST_KEY_PREFIX + userId;
        Long version = readVersion(key);
        if (version == null) {
            return 0;
        }
        if (version < 0) {
            redisTemplate.opsForValue().set(key, "1", tokenLifetime);
            return 1;
        }
        redisTemplate.expire(key, tokenLifetime);
        return version;
    }

    private Long readVersion(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }
}
