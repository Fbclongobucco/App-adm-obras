package com.longobuccodev.app_adm_obras.application.dto.audit;

import java.time.Instant;
import java.util.UUID;

/** Evento do log de alteracoes. {@code actorId} nulo com {@code actorName} SYSTEM: gravacao sem usuario. */
public record ChangeAuditResponseDTO(
        UUID id,
        String entityType,
        UUID entityId,
        String action,
        Instant occurredAt,
        UUID actorId,
        String actorName,
        String actorEmail
) {
}
