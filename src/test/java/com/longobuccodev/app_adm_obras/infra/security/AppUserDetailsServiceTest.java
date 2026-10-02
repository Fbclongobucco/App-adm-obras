package com.longobuccodev.app_adm_obras.infra.security;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppUserDetailsServiceTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuv";

    private UserRepository userRepository;
    private AppUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new AppUserDetailsService(userRepository);
    }

    @Test
    @DisplayName("deve derivar as autoridades das roles do usuario")
    void shouldMapRolesToAuthorities() {
        User user = user("admin@admim.com", true);
        user.addRole(Role.OPERADOR);
        when(userRepository.findByEmail("admin@admim.com")).thenReturn(user);

        var details = userDetailsService.loadUserByUsername("admin@admim.com");

        assertThat(details.getUsername()).isEqualTo("admin@admim.com");
        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_OPERADOR");
    }

    @Test
    @DisplayName("deve usar o email normalizado em minusculas como username")
    void shouldNormalizeUsername() {
        User user = user("admin@admim.com", true);
        when(userRepository.findByEmail("admin@admim.com")).thenReturn(user);

        assertThat(userDetailsService.loadUserByUsername("admin@admim.com").getUsername())
                .isEqualTo("admin@admim.com");
    }

    @Test
    @DisplayName("deve marcar usuario inativo como desabilitado")
    void shouldDisableInactiveUser() {
        when(userRepository.findByEmail("inativo@admim.com")).thenReturn(user("inativo@admim.com", false));

        assertThat(userDetailsService.loadUserByUsername("inativo@admim.com").isEnabled()).isFalse();
    }

    @Test
    @DisplayName("deve usar o hash bcrypt da senha, nunca a senha em claro")
    void shouldExposeHashedPassword() {
        when(userRepository.findByEmail("admin@admim.com")).thenReturn(user("admin@admim.com", true));

        assertThat(userDetailsService.loadUserByUsername("admin@admim.com").getPassword()).isEqualTo(HASH);
    }

    @Test
    @DisplayName("deve usar senha vazia quando o usuario nao tem hash, evitando NPE na checagem")
    void shouldFallBackToEmptyPasswordWhenHashIsAbsent() {
        User withoutHash = new User(null, "Sem Senha", "sem@admim.com", true);
        withoutHash.addRole(Role.OPERADOR);
        when(userRepository.findByEmail("sem@admim.com")).thenReturn(withoutHash);

        assertThat(userDetailsService.loadUserByUsername("sem@admim.com").getPassword()).isEmpty();
    }

    @Test
    @DisplayName("deve lancar UsernameNotFoundException para email desconhecido")
    void shouldThrowForUnknownEmail() {
        when(userRepository.findByEmail(any())).thenReturn(null);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ninguem@admim.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private User user(String email, boolean active) {
        User user = new User(null, "Administrador", email, active, HASH);
        user.addRole(Role.ADMIN);
        return user;
    }
}
