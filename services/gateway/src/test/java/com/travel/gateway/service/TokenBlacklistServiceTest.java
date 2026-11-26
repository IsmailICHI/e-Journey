package com.travel.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Mono;

class TokenBlacklistServiceTest {

    private final ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);

    @Test
    void rejectsTokensOlderThanTheCurrentUserTokenVersion() {
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(redisTemplate.hasKey("blacklist:token-value")).thenReturn(Mono.just(false));
        when(values.get("blacklist:user:42")).thenReturn(Mono.just("2"));
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, true);

        assertThat(service.isBlacklisted("token-value", "42", 1).block()).isTrue();

        verify(redisTemplate).hasKey("blacklist:token-value");
        verify(values).get("blacklist:user:42");
    }

    @Test
    void allowsTokensIssuedAtTheCurrentUserTokenVersion() {
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(redisTemplate.hasKey("blacklist:token-value")).thenReturn(Mono.just(false));
        when(values.get("blacklist:user:42")).thenReturn(Mono.just("2"));
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, true);

        assertThat(service.isBlacklisted("token-value", "42", 2).block()).isFalse();
    }

    @Test
    void rejectsTokensWhenTheLegacyRevocationMarkerHasNotBeenMigrated() {
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(redisTemplate.hasKey("blacklist:token-value")).thenReturn(Mono.just(false));
        when(values.get("blacklist:user:42")).thenReturn(Mono.just("revoked"));
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, true);

        assertThat(service.isBlacklisted("token-value", "42", 1).block()).isTrue();
    }
}
