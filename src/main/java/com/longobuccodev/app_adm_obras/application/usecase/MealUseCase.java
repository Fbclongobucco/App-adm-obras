package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.closing.WeeklyClosingResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.MealAdjustmentMapper;
import com.longobuccodev.app_adm_obras.application.mapper.MealMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.application.mapper.WeeklyClosingMapper;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.core.repository.RestaurantRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

    /** Listagem filtrada pelo termo de busca; vazio ou nulo nao filtra. */
    public PageResponseDTO<MealResponseDTO> findAll(PageRequest request, String query) {
        return PageMapper.toResponse(mealRepository.findAll(request, SearchTerm.of(query)), MealMapper::toResponse);
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

    /** Ajustes por data da refeicao, ordenados por data. */
    public List<MealAdjustmentResponseDTO> findAdjustments(UUID mealId) {
        return MealAdjustmentMapper.toResponses(findMeal(mealId));
    }

    /** Inclui o ajuste pelas regras da refeicao (dominio) e grava a refeicao. */
    public MealAdjustmentResponseDTO addAdjustment(UUID mealId, MealAdjustmentRequestDTO dto) {
        Meal meal = findMeal(mealId);
        MealAdjustment added = meal.addAdjustment(MealAdjustmentMapper.toDomain(dto));
        Instant now = clock.instant();
        added.markCreated(now);
        meal.touch(now);
        mealRepository.update(meal);
        return MealAdjustmentMapper.toResponse(added, meal);
    }

    /**
     * Grava o ajuste da data (a do caminho): substitui o existente, mantendo o id, ou inclui. Mesmas regras da
     * inclusao, sem o conflito de data.
     */
    public MealAdjustmentResponseDTO putAdjustment(UUID mealId, LocalDate date, MealAdjustmentRequestDTO dto) {
        Meal meal = findMeal(mealId);
        MealAdjustment saved = meal.putAdjustment(MealAdjustmentMapper.toDomain(dto, date));
        Instant now = clock.instant();
        saved.touch(now);
        meal.touch(now);
        mealRepository.update(meal);
        return MealAdjustmentMapper.toResponse(saved, meal);
    }

    public void removeAdjustment(UUID mealId, UUID adjustmentId) {
        Meal meal = findMeal(mealId);
        if (!meal.removeAdjustment(adjustmentId)) {
            throw ResourceNotFoundException.mealAdjustment(adjustmentId);
        }
        meal.touch(clock.instant());
        mealRepository.update(meal);
    }

    /** Semanas da obra e fechamento da semana que contem {@code week} (ou da semana padrao). */
    public WeeklyClosingResponseDTO weeklyClosing(UUID mealId, LocalDate week) {
        Meal meal = findMeal(mealId);
        return WeeklyClosingMapper.toResponse(meal.getProject(), List.of(meal), week, LocalDate.now(clock));
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

    /** Uma cobertura de almoco e uma de jantar por obra (regra do dominio); a obra vem do restaurante. */
    private void requireUniqueType(Meal meal) {
        boolean taken = mealRepository.findByProject(meal.getProject()).stream().anyMatch(meal::overlaps);
        Conflicts.requireFalse(taken,
                () -> ConflictException.duplicate("meal", "type", meal.getMealType().name()));
    }
}
