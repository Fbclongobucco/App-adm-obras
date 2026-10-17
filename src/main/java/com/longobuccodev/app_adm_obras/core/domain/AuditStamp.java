package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidAuditStampException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Instantes de criacao e de ultima alteracao de um registro de negocio.
 * Definidos pelos casos de uso a partir de um relogio injetado, nunca pelo cliente.
 */
public final class AuditStamp {

    private final Instant createdAt;
    private final Instant updatedAt;

    public AuditStamp(Instant createdAt, Instant updatedAt) {
        if (createdAt == null) {
            throw InvalidAuditStampException.missingCreatedAt();
        }
        if (updatedAt == null) {
            throw InvalidAuditStampException.missingUpdatedAt();
        }
        // Mesma precisao do banco (timestamp(6)), para a resposta da gravacao bater com a leitura
        createdAt = createdAt.truncatedTo(ChronoUnit.MICROS);
        updatedAt = updatedAt.truncatedTo(ChronoUnit.MICROS);
        if (updatedAt.isBefore(createdAt)) {
            throw InvalidAuditStampException.updatedBeforeCreated(createdAt, updatedAt);
        }
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AuditStamp createdAt(Instant now) {
        return new AuditStamp(now, now);
    }

    public AuditStamp touchedAt(Instant now) {
        return new AuditStamp(createdAt, now);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AuditStamp other
                && createdAt.equals(other.createdAt)
                && updatedAt.equals(other.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "AuditStamp[createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
