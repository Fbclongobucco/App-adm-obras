package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface RestaurantJpaRepository extends JpaRepository<RestaurantEntity, UUID>,
        JpaSpecificationExecutor<RestaurantEntity> {

    List<RestaurantEntity> findByProjectIdOrderByName(UUID projectId);
}
