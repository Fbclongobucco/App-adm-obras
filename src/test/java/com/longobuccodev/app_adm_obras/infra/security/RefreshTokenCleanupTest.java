package com.longobuccodev.app_adm_obras.infra.security;

import com.longobuccodev.app_adm_obras.core.domain.RefreshToken;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.infra.adapters.repository.RefreshTokenRepositoryAdapter;
import com.longobuccodev.app_adm_obras.infra.adapters.repository.UserRepositoryAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({RefreshTokenRepositoryAdapter.class, UserRepositoryAdapter.class})
class RefreshTokenCleanupTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

    @Autowired
    private RefreshTokenRepositoryAdapter tokens;

    @Autowired
    private UserRepositoryAdapter users;

    @Test
    @DisplayName("purgeExpired deve remover so os tokens expirados")
    void shouldRemoveOnlyExpired() {
        User user = new User(null, "Ana Souza", "ana.cleanup@email.com", true, "$2a$10$abcdefghijklmnopqrstuv");
        user.addRole(Role.OPERADOR);
        user = users.save(stamped(user));
        tokens.save(RefreshToken.issue(user.getId(), "a".repeat(64), NOW.minus(Duration.ofDays(8)),
                NOW.minus(Duration.ofDays(1))));
        tokens.save(RefreshToken.issue(user.getId(), "b".repeat(64), NOW.minus(Duration.ofDays(1)),
                NOW.plus(Duration.ofDays(6))));

        RefreshTokenCleanup cleanup = new RefreshTokenCleanup(tokens, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(cleanup.purgeExpired()).isEqualTo(1);
        assertThat(tokens.findByHash("a".repeat(64))).isNull();
        assertThat(tokens.findByHash("b".repeat(64))).isNotNull();
    }

    private static User stamped(User user) {
        user.markCreated(Instant.parse("2026-01-15T10:00:00Z"));
        return user;
    }
}
