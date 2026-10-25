package com.travel.user_service.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.travel.user_service.exceptions.ResourceNotFoundException;
import com.travel.user_service.models.User;
import com.travel.user_service.repository.UserRepository;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserTokenRevocationService tokenRevocationService = mock(UserTokenRevocationService.class);
    private final UserService userService = new UserService(userRepository, passwordEncoder, tokenRevocationService);

    @Test
    void revokesAllUserTokensBeforeDeletingTheAccount() {
        User user = new User();
        user.setId(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        userService.deleteUser(42L);

        var deletionOrder = inOrder(tokenRevocationService, userRepository);
        deletionOrder.verify(tokenRevocationService).revokeUserTokens(42L);
        deletionOrder.verify(userRepository).delete(user);
    }

    @Test
    void doesNotRevokeTokensWhenTheAccountDoesNotExist() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(42L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tokenRevocationService, never()).revokeUserTokens(42L);
    }
}
