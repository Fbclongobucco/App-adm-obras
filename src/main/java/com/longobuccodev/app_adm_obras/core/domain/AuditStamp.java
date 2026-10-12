package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidAuditStampException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Instantes de criacao e de ultima alteracao de um registro de negocio.
 * Definidos pelos casos de uso a partir de um relogio injetado, nunca pelo cliente.
 */
public record AuditStamp(Instant createdAt, Instant updatedAt) {

    public AuditStamp {
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
    }

    public static AuditStamp createdAt(Instant now) {
        return new AuditStamp(now, now);
    }

    public AuditStamp touchedAt(Instant now) {
        return new AuditStamp(createdAt, now);
    }
}
