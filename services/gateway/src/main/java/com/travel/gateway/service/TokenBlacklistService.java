package com.travel.gateway.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private final ReactiveStringRedisTemplate redisTemplate;
    private final boolean enabled;

    public TokenBlacklistService(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${app.security.token-blacklist.enabled:true}") boolean enabled) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        if (!enabled) {
            logger.warn("JWT token blacklist checks are disabled for this local-only profile.");
        }
    }

    public Mono<Boolean> isBlacklisted(String token) {
        if (!enabled) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey("blacklist:" + token)
                .defaultIfEmpty(false);
    }
}