package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.AccommodationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccommodationJpaRepository extends JpaRepository<AccommodationEntity, UUID> {

    Optional<AccommodationEntity> findFirstByHostName(String hostName);

    @Query("""
            SELECT accommodation FROM AccommodationEntity accommodation
            WHERE accommodation.project.id = :projectId
            """)
    List<AccommodationEntity> findByProjectId(@Param("projectId") UUID projectId);
}
