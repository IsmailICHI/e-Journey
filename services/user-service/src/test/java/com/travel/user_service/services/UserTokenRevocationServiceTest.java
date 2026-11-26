package com.travel.user_service.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class UserTokenRevocationServiceTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    @Test
    void incrementsUserTokenVersionToRevokeExistingTokens() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserTokenRevocationService service = new UserTokenRevocationService(redisTemplate, 3_600_000);

        service.revokeUserTokens(42L);

        verify(valueOperations).set("blacklist:user:42", "0");
        verify(valueOperations).increment("blacklist:user:42");
        verify(redisTemplate).expire("blacklist:user:42", java.time.Duration.ofHours(1));
    }

    @Test
    void issuesTheCurrentVersionForNewLoginTokens() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blacklist:user:42")).thenReturn("3");
        UserTokenRevocationService service = new UserTokenRevocationService(redisTemplate, 3_600_000);

        org.assertj.core.api.Assertions.assertThat(service.currentTokenVersion(42L)).isEqualTo(3);
        verify(redisTemplate).expire("blacklist:user:42", java.time.Duration.ofHours(1));
    }

    @Test
    void upgradesLegacyRevocationMarkersWithoutReenablingOldTokens() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blacklist:user:42")).thenReturn("revoked");
        UserTokenRevocationService service = new UserTokenRevocationService(redisTemplate, 3_600_000);

        org.assertj.core.api.Assertions.assertThat(service.currentTokenVersion(42L)).isEqualTo(1);

        verify(valueOperations).set("blacklist:user:42", "1", java.time.Duration.ofHours(1));
    }
}
