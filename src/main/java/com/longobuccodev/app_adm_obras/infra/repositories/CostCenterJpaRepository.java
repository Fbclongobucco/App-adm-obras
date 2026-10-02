package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.CostCenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CostCenterJpaRepository extends JpaRepository<CostCenterEntity, UUID> {

    Optional<CostCenterEntity> findFirstByName(String name);

    Optional<CostCenterEntity> findFirstByCnpj(String cnpj);
}
