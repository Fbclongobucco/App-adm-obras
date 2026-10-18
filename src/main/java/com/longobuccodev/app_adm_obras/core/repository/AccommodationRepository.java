package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AccommodationRepository {
    Accommodation save(Accommodation accommodation);
    Accommodation findById(UUID id);

    /** Os encontrados entre os ids, em uma leitura; ids inexistentes ficam de fora. */
    List<Accommodation> findAllById(Collection<UUID> ids);

    Accommodation findByHostName(String hostName);
    List<Accommodation> findAll();
    default Page<Accommodation> findAll(PageRequest request) {
        return findAll(request, SearchTerm.NONE);
    }

    /** Pagina filtrada pelo termo nos campos pesquisaveis do recurso; {@link SearchTerm#NONE} nao filtra. */
    Page<Accommodation> findAll(PageRequest request, SearchTerm term);
    List<Accommodation> findByProjectId(UUID projectId);

    boolean existsByEmployeeId(UUID employeeId);
    void delete(Accommodation accommodation);
    void deleteById(UUID id);
    void update(UUID id ,Accommodation accommodation);
}
