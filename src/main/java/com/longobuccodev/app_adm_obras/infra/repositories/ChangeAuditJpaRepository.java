package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.ChangeAuditEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

import java.util.UUID;

/** Somente leitura: sem save nem delete. */
public interface ChangeAuditJpaRepository extends Repository<ChangeAuditEntity, UUID>,
        JpaSpecificationExecutor<ChangeAuditEntity> {
}
