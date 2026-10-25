package com.travel.user_service.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class UserTokenRevocationServiceTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    @Test
    void blacklistsUserIdForAtLeastTheMaximumTokenLifetime() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserTokenRevocationService service = new UserTokenRevocationService(redisTemplate, 3_600_000);

        service.revokeUserTokens(42L);

        verify(valueOperations).set("blacklist:user:42", "revoked", Duration.ofHours(1));
    }
}
