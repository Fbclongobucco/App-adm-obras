package com.longobuccodev.app_adm_obras.infra.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    static final String VALID_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    @DisplayName("segredo Base64 com 32 bytes deve ser aceito, com validades padrao")
    void validSecretShouldBeAccepted() {
        JwtProperties properties = new JwtProperties(null, VALID_SECRET, null, null);

        assertThat(properties.secretBytes()).hasSize(32);
        assertThat(properties.issuer()).isEqualTo("app-adm-obras");
        assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.refreshTokenTtl()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    @DisplayName("segredo ausente deve falhar nomeando APP_JWT_SECRET")
    void missingSecretShouldFail() {
        assertThatThrownBy(() -> new JwtProperties(null, " ", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    @DisplayName("placeholder nao resolvido deve falhar nomeando APP_JWT_SECRET")
    void unresolvedPlaceholderShouldFail() {
        assertThatThrownBy(() -> new JwtProperties(null, "${APP_JWT_SECRET}", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured")
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    @DisplayName("segredo com menos de 32 bytes deve falhar nomeando APP_JWT_SECRET")
    void shortSecretShouldFail() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new JwtProperties(null, shortSecret, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET")
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("segredo que nao e Base64 deve falhar nomeando APP_JWT_SECRET")
    void invalidBase64ShouldFail() {
        assertThatThrownBy(() -> new JwtProperties(null, "nao-e-base64!!", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    @DisplayName("validade do refresh token deve ser maior que a do access token")
    void refreshTtlShouldExceedAccessTtl() {
        assertThatThrownBy(() -> new JwtProperties(null, VALID_SECRET, Duration.ofHours(1), Duration.ofMinutes(30)))
                .isInstanceOf(IllegalStateException.class);
    }
}
