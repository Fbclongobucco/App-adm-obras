package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;
import com.longobuccodev.app_adm_obras.core.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");
    private static final Instant EXPIRY = NOW.plus(Duration.ofDays(7));

    private static RefreshToken newToken() {
        return RefreshToken.issue(UUID.randomUUID(), "hash-1", NOW, EXPIRY);
    }

    @Test
    void shouldBeActiveBeforeExpiry() {
        RefreshToken token = newToken();

        assertThat(token.isActive(NOW)).isTrue();
        assertThat(token.isActive(EXPIRY.minusSeconds(1))).isTrue();
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    void shouldBeInactiveAtAndAfterExpiry() {
        RefreshToken token = newToken();

        assertThat(token.isActive(EXPIRY)).isFalse();
        assertThat(token.isExpired(EXPIRY)).isTrue();
    }

    @Test
    void shouldBeInactiveOnceRevoked() {
        RefreshToken token = newToken();

        token.revoke(RevokeReason.ROTATED, NOW.plusSeconds(10));

        assertThat(token.isActive(NOW.plusSeconds(20))).isFalse();
        assertThat(token.wasRotated()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void shouldKeepFirstRevocation() {
        RefreshToken token = newToken();

        token.revoke(RevokeReason.ROTATED, NOW.plusSeconds(10));
        token.revoke(RevokeReason.REUSE, NOW.plusSeconds(20));

        assertThat(token.getRevokeReason()).isEqualTo(RevokeReason.ROTATED);
        assertThat(token.getRevokedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void successorShouldKeepFamilyAndAbsoluteExpiry() {
        RefreshToken token = newToken();

        RefreshToken next = token.successor("hash-2", NOW.plus(Duration.ofDays(3)));

        assertThat(next.getId()).isNotEqualTo(token.getId());
        assertThat(next.getFamilyId()).isEqualTo(token.getFamilyId());
        assertThat(next.getUserId()).isEqualTo(token.getUserId());
        assertThat(next.getExpiresAt()).isEqualTo(EXPIRY);
        assertThat(next.getTokenHash()).isEqualTo("hash-2");
    }

    @Test
    void shouldRejectMissingFields() {
        UUID user = UUID.randomUUID();
        UUID family = UUID.randomUUID();

        assertThatThrownBy(() -> new RefreshToken(null, null, "h", family, EXPIRY, NOW, null, null))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> new RefreshToken(null, user, " ", family, EXPIRY, NOW, null, null))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> new RefreshToken(null, user, "h", null, EXPIRY, NOW, null, null))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> new RefreshToken(null, user, "h", family, null, NOW, null, null))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> new RefreshToken(null, user, "h", family, EXPIRY, null, null, null))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> new RefreshToken(null, user, "h", family, EXPIRY, NOW, NOW, null))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void shouldRejectExpiryNotAfterCreation() {
        assertThatThrownBy(() -> RefreshToken.issue(UUID.randomUUID(), "h", NOW, NOW))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
