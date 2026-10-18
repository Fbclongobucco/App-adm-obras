package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.EntityType;

import java.time.Instant;
import java.util.UUID;

/** Leitura do log de alteracoes. A gravacao acontece na persistencia, junto com cada alteracao. */
public interface ChangeAuditRepository {

    /**
     * Eventos do mais recente para o mais antigo. Filtros nulos nao filtram; {@code from} inclusivo,
     * {@code to} exclusivo.
     */
    Page<ChangeAuditEntry> find(EntityType entityType, UUID entityId, UUID actorId, Instant from, Instant to,
                                PageRequest request);
}
