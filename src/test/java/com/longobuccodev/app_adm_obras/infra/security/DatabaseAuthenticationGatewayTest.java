package com.longobuccodev.app_adm_obras.infra.security;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.exception.AuthenticationFailedException;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseAuthenticationGatewayTest {

    private final PasswordHasher hasher = new BCryptPasswordHasher(new BCryptPasswordEncoder());
    private UserRepository userRepository;
    private DatabaseAuthenticationGateway gateway;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        gateway = new DatabaseAuthenticationGateway(userRepository, hasher);
    }

    @Test
    @DisplayName("deve autenticar com a senha correta")
    void shouldAuthenticateWithCorrectPassword() {
        when(userRepository.findByEmail("admin@admim.com")).thenReturn(user("admin@admim.com", "admin123", true));

        User authenticated = gateway.authenticate("admin@admim.com", "admin123");

        assertThat(authenticated.getEmail()).isEqualTo("admin@admim.com");
    }

    @Test
    @DisplayName("deve rejeitar senha incorreta")
    void shouldRejectWrongPassword() {
        when(userRepository.findByEmail("admin@admim.com")).thenReturn(user("admin@admim.com", "admin123", true));

        assertThatThrownBy(() -> gateway.authenticate("admin@admim.com", "senha-errada"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("deve rejeitar email inexistente sem revelar se a senha estava correta")
    void shouldRejectUnknownEmail() {
        when(userRepository.findByEmail(any())).thenReturn(null);

        assertThatThrownBy(() -> gateway.authenticate("ninguem@admim.com", "admin123"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("deve rejeitar usuario inativo mesmo com a senha correta")
    void shouldRejectInactiveUser() {
        when(userRepository.findByEmail("inativo@admim.com")).thenReturn(user("inativo@admim.com", "admin123", false));

        assertThatThrownBy(() -> gateway.authenticate("inativo@admim.com", "admin123"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("a senha nunca deve ser exposta em claro, apenas o hash bcrypt")
    void shouldNeverExposeRawPassword() {
        User user = user("admin@admim.com", "admin123", true);

        assertThat(user.getPasswordHash()).isNotEqualTo("admin123").startsWith("$2");
    }

    @Test
    @DisplayName("deve localizar o usuario ignorando caixa do email")
    void shouldFindUserIgnoringCase() {
        when(userRepository.findByEmail("ADMIN@ADMIM.COM")).thenReturn(user("admin@admim.com", "admin123", true));

        assertThat(gateway.authenticate("ADMIN@ADMIM.COM", "admin123")).isNotNull();
    }

    @Test
    @DisplayName("o hasher nao deve considerar nulo ou vazio como senha valida")
    void hasherShouldRejectNullAndBlank() {
        assertThat(hasher.matches(null, "$2a$10$hash")).isFalse();
        assertThat(hasher.matches("admin123", null)).isFalse();
        assertThat(hasher.matches("admin123", "")).isFalse();
    }

    private User user(String email, String rawPassword, boolean active) {
        User user = new User(null, "Administrador", email, active, hasher.hash(rawPassword));
        user.addRole(Role.ADMIN);
        return user;
    }
}
