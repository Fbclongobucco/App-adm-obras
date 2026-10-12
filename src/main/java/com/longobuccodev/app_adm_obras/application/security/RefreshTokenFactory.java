package com.longobuccodev.app_adm_obras.application.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Gera o valor opaco do refresh token e o hash que e persistido. O valor tem
 * 256 bits aleatorios; SHA-256 basta e permite busca pelo hash.
 */
public class RefreshTokenFactory {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random;

    public RefreshTokenFactory() {
        this(new SecureRandom());
    }

    public RefreshTokenFactory(SecureRandom random) {
        this.random = random;
    }

    public String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
