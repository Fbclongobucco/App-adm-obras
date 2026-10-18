package com.longobuccodev.app_adm_obras.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de auditoria: quem criou, alterou ou excluiu um registro de negocio, e quando. Nao guarda valores
 * de campos. O autor e registrado com nome e e-mail do instante; sem usuario autenticado, e {@link #SYSTEM}.
 */
public final class ChangeAuditEntry {

    /** Nome do autor quando a gravacao nao vem de uma requisicao autenticada (rotina interna, bootstrap). */
    public static final String SYSTEM = "SYSTEM";

    private final UUID id;
    private final EntityType entityType;
    private final UUID entityId;
    private final Action action;
    private final Instant occurredAt;
    private final UUID actorId;
    private final String actorName;
    private final String actorEmail;

    public ChangeAuditEntry(UUID id, EntityType entityType, UUID entityId, Action action, Instant occurredAt,
                            UUID actorId, String actorName, String actorEmail) {
        this.id = Objects.requireNonNull(id, "id");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.action = Objects.requireNonNull(action, "action");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.actorId = actorId;
        this.actorName = Objects.requireNonNull(actorName, "actorName");
        this.actorEmail = actorEmail;
    }

    public UUID getId() {
        return id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Action getAction() {
        return action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public enum EntityType {
        PROJECT,
        EMPLOYEE,
        COST_CENTER,
        CLIENT,
        ACCOMMODATION,
        ACCOMMODATION_PERIOD,
        MEAL,
        MEAL_ADJUSTMENT,
        RESTAURANT,
        USER
    }

    public enum Action {
        CREATE,
        UPDATE,
        DELETE
    }
}
