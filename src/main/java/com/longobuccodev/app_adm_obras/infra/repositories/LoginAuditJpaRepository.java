package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.LoginAuditEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

import java.util.UUID;

/** Grava e le registros de login; sem update nem delete. */
public interface LoginAuditJpaRepository extends Repository<LoginAuditEntity, UUID>,
        JpaSpecificationExecutor<LoginAuditEntity> {

    LoginAuditEntity save(LoginAuditEntity entity);
}
