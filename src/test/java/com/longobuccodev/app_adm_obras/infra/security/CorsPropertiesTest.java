package com.longobuccodev.app_adm_obras.infra.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPropertiesTest {

    @Test
    @DisplayName("lista explicita de origens deve ser aceita")
    void explicitOriginsShouldBeAccepted() {
        CorsProperties properties = new CorsProperties(List.of("http://localhost:3000", "https://app.exemplo.com"));

        assertThat(properties.allows("https://app.exemplo.com")).isTrue();
        assertThat(properties.allows("https://outra.exemplo.com")).isFalse();
        assertThat(properties.allows(null)).isFalse();
    }

    @Test
    @DisplayName("curinga deve falhar nomeando APP_CORS_ALLOWED_ORIGINS")
    void wildcardShouldFail() {
        assertThatThrownBy(() -> new CorsProperties(List.of("http://localhost:3000", "*")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_CORS_ALLOWED_ORIGINS");
    }
}
