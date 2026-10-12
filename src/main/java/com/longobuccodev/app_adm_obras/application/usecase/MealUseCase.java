package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.meal.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.MealMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
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

public class MealUseCase {

    private final MealRepository mealRepository;
    private final ProjectRepository projectRepository;

    private final RestaurantRepository restaurantRepository;
    private final Clock clock;

    public MealUseCase(MealRepository mealRepository, ProjectRepository projectRepository,
                       RestaurantRepository restaurantRepository, Clock clock) {
        this.mealRepository = mealRepository;
        this.projectRepository = projectRepository;
        this.restaurantRepository = restaurantRepository;
        this.clock = clock;
    }

    public MealResponseDTO create(MealRequestDTO dto) {
        Meal meal = MealMapper.toDomain(dto, findRestaurant(dto.restaurantId()));
        requireUniqueType(meal);
        meal.markCreated(clock.instant());
        return MealMapper.toResponse(mealRepository.save(meal));
    }

    public MealResponseDTO findById(UUID id) {
        return MealMapper.toResponse(findMeal(id));
    }

    public List<MealResponseDTO> findAll() {
        return mealRepository.findAll().stream()
                .map(MealMapper::toResponse)
                .toList();
    }

    public PageResponseDTO<MealResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(mealRepository.findAll(request), MealMapper::toResponse);
    }

    public List<MealResponseDTO> findByProjectId(UUID projectId) {
        Project project = EntityLookup.require(projectId, projectRepository::findById,
                ResourceNotFoundException::project);
        return mealRepository.findByProject(project).stream()
                .map(MealMapper::toResponse)
                .toList();
    }

    public MealResponseDTO update(UUID id, MealRequestDTO dto) {
        Meal existing = findMeal(id);
        Meal updated = MealMapper.toDomain(existing, dto, findRestaurant(dto.restaurantId()));
        requireUniqueType(updated);
        updated.touchFrom(existing, clock.instant());
        mealRepository.update(updated);
        return MealMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findMeal(id);
        mealRepository.deleteById(id);
    }

    private Meal findMeal(UUID id) {
        return EntityLookup.require(id, mealRepository::findById, ResourceNotFoundException::meal);
    }

    private Restaurant findRestaurant(UUID restaurantId) {
        return EntityLookup.optional(restaurantId, restaurantRepository::findById,
                ResourceNotFoundException::restaurant);
    }

    /** Uma refeicao de cada tipo por obra; a obra vem do restaurante. */
    private void requireUniqueType(Meal meal) {
        boolean taken = mealRepository.findByProject(meal.getProject()).stream()
                .anyMatch(other -> other.getMealType() == meal.getMealType() && !other.getId().equals(meal.getId()));
        Conflicts.requireFalse(taken,
                () -> ConflictException.duplicate("meal", "type", meal.getMealType().name()));
    }
}
