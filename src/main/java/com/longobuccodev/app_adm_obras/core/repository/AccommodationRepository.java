package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Accommodation;

import java.util.List;
import java.util.UUID;

public interface AccommodationRepository {
    Accommodation save(Accommodation accommodation);
    Accommodation findById(UUID id);
    Accommodation findByHostName(String hostName);
    List<Accommodation> findAll();
    Page<Accommodation> findAll(PageRequest request);
    List<Accommodation> findByProjectId(UUID projectId);
    void delete(Accommodation accommodation);
    void deleteById(UUID id);
    void update(UUID id ,Accommodation accommodation);
}
