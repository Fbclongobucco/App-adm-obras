package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.application.mapper.RestaurantMapper;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.core.repository.RestaurantRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class RestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final ProjectRepository projectRepository;
    private final MealRepository mealRepository;
    private final Clock clock;

    public RestaurantUseCase(RestaurantRepository restaurantRepository, ProjectRepository projectRepository,
                             MealRepository mealRepository, Clock clock) {
        this.restaurantRepository = restaurantRepository;
        this.projectRepository = projectRepository;
        this.mealRepository = mealRepository;
        this.clock = clock;
    }

    public RestaurantResponseDTO create(RestaurantRequestDTO dto) {
        Restaurant restaurant = RestaurantMapper.toDomain(dto, findProject(dto.projectId()));
        restaurant.markCreated(clock.instant());
        return RestaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    public RestaurantResponseDTO findById(UUID id) {
        return RestaurantMapper.toResponse(findRestaurant(id));
    }

    public PageResponseDTO<RestaurantResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(restaurantRepository.findAll(request), RestaurantMapper::toResponse);
    }

    public List<RestaurantResponseDTO> findByProjectId(UUID projectId) {
        EntityLookup.require(projectId, projectRepository::findById, ResourceNotFoundException::project);
        return restaurantRepository.findByProjectId(projectId).stream()
                .map(RestaurantMapper::toResponse)
                .toList();
    }

    public RestaurantResponseDTO update(UUID id, RestaurantRequestDTO dto) {
        Restaurant existing = findRestaurant(id);
        Restaurant updated = RestaurantMapper.toDomain(existing, dto, findProject(dto.projectId()));
        // As refeicoes herdam a obra do restaurante; mover o restaurante as levaria junto
        boolean projectChanged = !existing.getProject().getId().equals(updated.getProject().getId());
        Conflicts.requireFalse(projectChanged && mealRepository.existsByRestaurantId(id),
                () -> ConflictException.inUse("restaurant", "meals"));
        updated.touchFrom(existing, clock.instant());
        restaurantRepository.update(updated);
        return RestaurantMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findRestaurant(id);
        Conflicts.requireFalse(mealRepository.existsByRestaurantId(id),
                () -> ConflictException.inUse("restaurant", "meals"));
        restaurantRepository.deleteById(id);
    }

    private Restaurant findRestaurant(UUID id) {
        return EntityLookup.require(id, restaurantRepository::findById, ResourceNotFoundException::restaurant);
    }

    private Project findProject(UUID projectId) {
        return EntityLookup.optional(projectId, projectRepository::findById, ResourceNotFoundException::project);
    }
}
