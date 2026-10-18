package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.EntityType;
import com.longobuccodev.app_adm_obras.core.repository.ChangeAuditRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.entities.ChangeAuditEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.ChangeAuditJpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public class ChangeAuditRepositoryAdapter implements ChangeAuditRepository {

    private final ChangeAuditJpaRepository jpaRepository;

    public ChangeAuditRepositoryAdapter(ChangeAuditJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChangeAuditEntry> find(EntityType entityType, UUID entityId, UUID actorId, Instant from, Instant to,
                                       PageRequest request) {
        Specification<ChangeAuditEntity> filter = Specification.allOf(
                AuditQuerySupport.equal("entityType", entityType),
                AuditQuerySupport.equal("entityId", entityId),
                AuditQuerySupport.equal("actorId", actorId),
                AuditQuerySupport.between(from, to));
        return SpringPageSupport.toDomain(jpaRepository.findAll(filter, AuditQuerySupport.newestFirst(request)),
                ChangeAuditRepositoryAdapter::toDomain);
    }

    private static ChangeAuditEntry toDomain(ChangeAuditEntity entity) {
        return new ChangeAuditEntry(entity.getId(), entity.getEntityType(), entity.getEntityId(), entity.getAction(),
                entity.getOccurredAt(), entity.getActorId(), entity.getActorName(), entity.getActorEmail());
    }
}
