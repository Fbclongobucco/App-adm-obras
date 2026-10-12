package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;

import java.util.List;
import java.util.UUID;

public interface RestaurantRepository {

    Restaurant save(Restaurant restaurant);

    Restaurant findById(UUID id);

    default Page<Restaurant> findAll(PageRequest request) {
        return findAll(request, SearchTerm.NONE);
    }

    /** Pagina filtrada pelo termo nos campos pesquisaveis do recurso; {@link SearchTerm#NONE} nao filtra. */
    Page<Restaurant> findAll(PageRequest request, SearchTerm term);

    List<Restaurant> findByProjectId(UUID projectId);

    void update(Restaurant restaurant);

    void deleteById(UUID id);
}
