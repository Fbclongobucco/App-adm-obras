package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.user.UserRequestDTO;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.repository.RefreshTokenRepository;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private UserUseCase useCase;
    private User current;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordHasher hasher = mock(PasswordHasher.class);
        when(hasher.hash(anyString())).thenAnswer(invocation -> "$2a$10$hash-de-" + invocation.getArgument(0));
        useCase = new UserUseCase(userRepository, hasher, refreshTokenRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        current = new User(null, "Ana Souza", "ana@email.com", true, "$2a$10$hash-atual");
        current.addRole(Role.OPERADOR);
        when(userRepository.findById(current.getId())).thenReturn(current);
    }

    private UserRequestDTO request(String name, boolean active, String password) {
        return new UserRequestDTO(name, "ana@email.com", active, Set.of(Role.OPERADOR), password);
    }

    @Test
    @DisplayName("troca de senha deve revogar os refresh tokens do usuario")
    void passwordChangeShouldRevoke() {
        useCase.update(current.getId(), request("Ana Souza", true, "novaSenha1"));

        verify(refreshTokenRepository).revokeAllByUser(current.getId(), RevokeReason.USER_CHANGED, NOW);
    }

    @Test
    @DisplayName("desativacao deve revogar os refresh tokens do usuario")
    void deactivationShouldRevoke() {
        useCase.update(current.getId(), request("Ana Souza", false, null));

        verify(refreshTokenRepository).revokeAllByUser(current.getId(), RevokeReason.USER_CHANGED, NOW);
    }

    @Test
    @DisplayName("mudanca so de nome nao deve revogar")
    void nameChangeShouldNotRevoke() {
        useCase.update(current.getId(), request("Ana Souza Lima", true, null));

        verify(refreshTokenRepository, never()).revokeAllByUser(any(), any(), any());
    }

    @Test
    @DisplayName("reativacao nao deve revogar")
    void reactivationShouldNotRevoke() {
        current.setActive(false);

        useCase.update(current.getId(), request("Ana Souza", true, " "));

        verify(refreshTokenRepository, never()).revokeAllByUser(any(), any(), any());
    }
}
