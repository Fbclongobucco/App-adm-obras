package com.longobuccodev.app_adm_obras.infra.audit;

import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.Action;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.EntityType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Grava um evento em change_audit para cada insert, update e delete de entidade auditada, na mesma conexao
 * e transacao do flush: a alteracao e o evento confirmam ou desfazem juntos. JDBC direto porque o
 * Hibernate nao aceita novas entidades na fila durante o flush. Update so dispara para entidade suja.
 */
final class ChangeAuditListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final String INSERT = "insert into change_audit (id, entity_type, entity_id, action, occurred_at, "
            + "actor_id, actor_name, actor_email) values (?, ?, ?, ?, ?, ?, ?, ?)";

    private final Clock clock;

    ChangeAuditListener(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        record(event.getSession(), event.getEntity(), event.getId(), Action.CREATE);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        record(event.getSession(), event.getEntity(), event.getId(), Action.UPDATE);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        record(event.getSession(), event.getEntity(), event.getId(), Action.DELETE);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private void record(SharedSessionContractImplementor session, Object entity, Object id, Action action) {
        EntityType type = AuditedEntityTypes.of(entity).orElse(null);
        if (type == null) {
            return;
        }
        session.doWork(connection -> {
            AuditActorResolver.Actor actor = AuditActorResolver.resolve(connection);
            try (PreparedStatement insert = connection.prepareStatement(INSERT)) {
                insert.setObject(1, UUID.randomUUID());
                insert.setString(2, type.name());
                insert.setObject(3, id);
                insert.setString(4, action.name());
                // Mesma precisao das demais colunas de instante (timestamp(6))
                insert.setObject(5, clock.instant().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC));
                insert.setObject(6, actor.id());
                insert.setString(7, actor.name());
                insert.setString(8, actor.email());
                insert.executeUpdate();
            }
        });
    }
}
