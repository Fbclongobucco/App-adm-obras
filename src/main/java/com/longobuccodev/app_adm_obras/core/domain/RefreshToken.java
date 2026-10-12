package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidRefreshTokenException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token persistido. Guarda apenas o hash do valor entregue ao cliente.
 * Tokens do mesmo login compartilham {@code familyId} e {@code expiresAt}.
 */
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant revokedAt;
    private RevokeReason revokeReason;

    public RefreshToken(UUID id, UUID userId, String tokenHash, UUID familyId, Instant expiresAt,
                        Instant createdAt, Instant revokedAt, RevokeReason revokeReason) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.userId = require(userId, "user");
        this.tokenHash = requireText(tokenHash);
        this.familyId = require(familyId, "family");
        this.expiresAt = require(expiresAt, "expiry");
        this.createdAt = require(createdAt, "creation");
        if (!expiresAt.isAfter(createdAt)) {
            throw InvalidRefreshTokenException.expiryBeforeCreation();
        }
        this.revokedAt = revokedAt;
        this.revokeReason = revokedAt == null ? null : require(revokeReason, "reason");
    }

    /** Primeiro token de um login: abre uma familia nova. */
    public static RefreshToken issue(UUID userId, String tokenHash, Instant now, Instant expiresAt) {
        return new RefreshToken(null, userId, tokenHash, UUID.randomUUID(), expiresAt, now, null, null);
    }

    /** Sucessor na rotacao: mesma familia e mesma expiracao absoluta. */
    public RefreshToken successor(String newTokenHash, Instant now) {
        return new RefreshToken(null, userId, newTokenHash, familyId, expiresAt, now, null, null);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean wasRotated() {
        return revokeReason == RevokeReason.ROTATED;
    }

    public void revoke(RevokeReason reason, Instant now) {
        if (revokedAt == null) {
            revokedAt = require(now, "revocation");
            revokeReason = require(reason, "reason");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public RevokeReason getRevokeReason() {
        return revokeReason;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof RefreshToken token && Objects.equals(id, token.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw InvalidRefreshTokenException.missingField(field);
        }
        return value;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw InvalidRefreshTokenException.missingField("hash");
        }
        return value;
    }

    public enum RevokeReason {
        ROTATED,
        LOGOUT,
        REUSE,
        USER_CHANGED
    }
}
