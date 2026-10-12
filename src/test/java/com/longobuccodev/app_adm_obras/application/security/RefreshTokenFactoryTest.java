package com.longobuccodev.app_adm_obras.application.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenFactoryTest {

    private final RefreshTokenFactory factory = new RefreshTokenFactory();

    @Test
    void tokensShouldNotRepeat() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            tokens.add(factory.newToken());
        }

        assertThat(tokens).hasSize(1_000);
    }

    @Test
    void tokenShouldCarry256BitsInUrlSafeBase64() {
        String token = factory.newToken();

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void hashShouldBeDeterministicHexOf64Chars() {
        String token = factory.newToken();

        assertThat(factory.hash(token)).isEqualTo(factory.hash(token)).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void hashShouldDifferFromToken() {
        String token = factory.newToken();

        assertThat(factory.hash(token)).isNotEqualTo(token);
        assertThat(factory.hash(factory.hash(token))).isNotEqualTo(factory.hash(token));
    }
}
