package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.RefreshToken;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({RefreshTokenRepositoryAdapter.class, UserRepositoryAdapter.class})
class RefreshTokenRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");
    private static final Instant EXPIRY = NOW.plus(Duration.ofDays(7));

    @Autowired
    private RefreshTokenRepositoryAdapter adapter;

    @Autowired
    private UserRepositoryAdapter users;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(null, "Maria Souza", "maria." + UUID.randomUUID() + "@email.com", true,
                "$2a$10$abcdefghijklmnopqrstuv");
        user.addRole(Role.OPERADOR);
        user = users.save(stamped(user));
    }

    private RefreshToken saveToken(String hash) {
        return adapter.save(RefreshToken.issue(user.getId(), hash, NOW, EXPIRY));
    }

    @Test
    @DisplayName("save e findByHash devem preservar todos os campos")
    void saveAndFindByHash() {
        RefreshToken saved = saveToken("a".repeat(64));

        RefreshToken found = adapter.findByHash("a".repeat(64));

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getFamilyId()).isEqualTo(saved.getFamilyId());
        assertThat(found.getExpiresAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(EXPIRY);
        assertThat(found.isRevoked()).isFalse();
        assertThat(adapter.findByHash("b".repeat(64))).isNull();
    }

    @Test
    @DisplayName("markRotatedIfActive deve funcionar so na primeira chamada")
    void markRotatedOnlyOnce() {
        RefreshToken saved = saveToken("c".repeat(64));

        assertThat(adapter.markRotatedIfActive(saved.getId(), NOW.plusSeconds(1))).isTrue();
        assertThat(adapter.markRotatedIfActive(saved.getId(), NOW.plusSeconds(2))).isFalse();

        RefreshToken reloaded = adapter.findByHash("c".repeat(64));
        assertThat(reloaded.wasRotated()).isTrue();
    }

    @Test
    @DisplayName("revokeFamily nao deve afetar outra familia")
    void revokeFamilyKeepsOtherFamilies() {
        RefreshToken first = saveToken("d".repeat(64));
        adapter.save(first.successor("e".repeat(64), NOW.plusSeconds(5)));
        saveToken("f".repeat(64));

        adapter.revokeFamily(first.getFamilyId(), RevokeReason.REUSE, NOW.plusSeconds(10));

        assertThat(adapter.findByHash("d".repeat(64)).getRevokeReason()).isEqualTo(RevokeReason.REUSE);
        assertThat(adapter.findByHash("e".repeat(64)).getRevokeReason()).isEqualTo(RevokeReason.REUSE);
        assertThat(adapter.findByHash("f".repeat(64)).isRevoked()).isFalse();
    }

    @Test
    @DisplayName("isFamilyClosed deve considerar aberta a familia so com tokens trocados e ativos")
    void familyWithRotatedAndActiveTokensIsOpen() {
        RefreshToken first = saveToken("6".repeat(64));
        adapter.markRotatedIfActive(first.getId(), NOW.plusSeconds(1));
        adapter.save(first.successor("7".repeat(64), NOW.plusSeconds(1)));

        assertThat(adapter.isFamilyClosed(first.getFamilyId())).isFalse();
    }

    @Test
    @DisplayName("isFamilyClosed deve considerar fechada a familia revogada por logout, reuso ou usuario alterado")
    void familyRevokedForOtherReasonsIsClosed() {
        for (RevokeReason reason : new RevokeReason[]{RevokeReason.LOGOUT, RevokeReason.REUSE,
                RevokeReason.USER_CHANGED}) {
            String prefix = reason.name().substring(0, 1).toLowerCase();
            RefreshToken first = saveToken(prefix + "8".repeat(63));
            adapter.markRotatedIfActive(first.getId(), NOW.plusSeconds(1));
            adapter.save(first.successor(prefix + "9".repeat(63), NOW.plusSeconds(1)));

            adapter.revokeFamily(first.getFamilyId(), reason, NOW.plusSeconds(2));

            assertThat(adapter.isFamilyClosed(first.getFamilyId())).as(reason.name()).isTrue();
        }
    }

    @Test
    @DisplayName("revokeAllByUser deve revogar todos os tokens do usuario e preservar revogacoes anteriores")
    void revokeAllByUser() {
        RefreshToken rotated = saveToken("1".repeat(64));
        adapter.markRotatedIfActive(rotated.getId(), NOW.plusSeconds(1));
        saveToken("2".repeat(64));

        adapter.revokeAllByUser(user.getId(), RevokeReason.USER_CHANGED, NOW.plusSeconds(10));

        assertThat(adapter.findByHash("1".repeat(64)).getRevokeReason()).isEqualTo(RevokeReason.ROTATED);
        assertThat(adapter.findByHash("2".repeat(64)).getRevokeReason()).isEqualTo(RevokeReason.USER_CHANGED);
    }

    @Test
    @DisplayName("deleteExpiredBefore deve remover so os expirados")
    void deleteExpiredBefore() {
        adapter.save(RefreshToken.issue(user.getId(), "3".repeat(64), NOW.minus(Duration.ofDays(8)),
                NOW.minus(Duration.ofDays(1))));
        saveToken("4".repeat(64));

        assertThat(adapter.deleteExpiredBefore(NOW)).isEqualTo(1);
        assertThat(adapter.findByHash("3".repeat(64))).isNull();
        assertThat(adapter.findByHash("4".repeat(64))).isNotNull();
    }

    @Test
    @DisplayName("apagar o usuario deve apagar os tokens dele")
    void deletingUserCascades() {
        saveToken("5".repeat(64));

        users.deleteById(user.getId());
        users.findById(user.getId());

        assertThat(adapter.findByHash("5".repeat(64))).isNull();
    }

    private static User stamped(User user) {
        user.markCreated(Instant.parse("2026-01-15T10:00:00Z"));
        return user;
    }
}
