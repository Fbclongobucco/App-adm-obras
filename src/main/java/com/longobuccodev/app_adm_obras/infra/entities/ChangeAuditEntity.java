package com.longobuccodev.app_adm_obras.infra.entities;

import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.Action;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.EntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Somente leitura: as linhas sao gravadas pelo listener de auditoria, por JDBC, durante o flush. */
@Entity
@Immutable
@Table(name = "change_audit")
public class ChangeAuditEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    // VARCHAR em todos os bancos, como nas migracoes; sem isso o H2Dialect usa ENUM nativo
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "entity_type", nullable = false, length = 30)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "action", nullable = false, length = 10)
    private Action action;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(name = "actor_email")
    private String actorEmail;

    protected ChangeAuditEntity() {
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
}
