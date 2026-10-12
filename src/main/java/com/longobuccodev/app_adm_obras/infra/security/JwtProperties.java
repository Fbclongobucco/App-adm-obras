package com.longobuccodev.app_adm_obras.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Base64;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String issuer, String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {

    static final String SECRET_VARIABLE = "APP_JWT_SECRET";
    static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        issuer = issuer == null || issuer.isBlank() ? "app-adm-obras" : issuer.strip();
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
        if (accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalStateException("app.security.jwt.access-token-ttl must be positive");
        }
        if (refreshTokenTtl.compareTo(accessTokenTtl) <= 0) {
            throw new IllegalStateException(
                    "app.security.jwt.refresh-token-ttl must be greater than app.security.jwt.access-token-ttl");
        }
        decode(secret);
    }

    /** Segredo HS256 decodificado; validado no construtor. */
    public byte[] secretBytes() {
        return decode(secret);
    }

    private static byte[] decode(String secret) {
        // Placeholder sem valor (${APP_JWT_SECRET}) chega literal quando a variavel nao existe
        if (secret == null || secret.isBlank() || secret.contains("${")) {
            throw new IllegalStateException(
                    "JWT signing secret is not configured: set " + SECRET_VARIABLE + " (Base64, at least "
                            + MIN_SECRET_BYTES + " bytes)");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret.strip());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(SECRET_VARIABLE + " must be valid Base64: " + ex.getMessage());
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(SECRET_VARIABLE + " must decode to at least " + MIN_SECRET_BYTES
                    + " bytes, got " + bytes.length);
        }
        return bytes;
    }
}
