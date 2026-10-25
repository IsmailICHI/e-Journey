package com.travel.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import reactor.core.publisher.Mono;

class TokenBlacklistServiceTest {

    private final ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);

    @Test
    void checksBothTokenAndUserRevocationKeys() {
        when(redisTemplate.hasKey("blacklist:token-value")).thenReturn(Mono.just(false));
        when(redisTemplate.hasKey("blacklist:user:42")).thenReturn(Mono.just(true));
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, true);

        assertThat(service.isBlacklisted("token-value", "42").block()).isTrue();

        verify(redisTemplate).hasKey("blacklist:token-value");
        verify(redisTemplate).hasKey("blacklist:user:42");
    }
}
