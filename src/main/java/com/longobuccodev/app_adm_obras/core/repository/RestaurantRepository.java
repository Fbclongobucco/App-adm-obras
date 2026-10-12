package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Restaurant;

import java.util.List;
import java.util.UUID;

public interface RestaurantRepository {

    Restaurant save(Restaurant restaurant);

    Restaurant findById(UUID id);

    Page<Restaurant> findAll(PageRequest request);

    List<Restaurant> findByProjectId(UUID projectId);

    void update(Restaurant restaurant);

    void deleteById(UUID id);
}
