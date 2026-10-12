package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, UUID>,
        JpaSpecificationExecutor<ClientEntity> {

    Optional<ClientEntity> findFirstByEmailIgnoreCase(String email);
}
