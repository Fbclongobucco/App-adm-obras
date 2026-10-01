package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.MealMapper;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;

import java.util.List;
import java.util.UUID;

public class MealUseCase {

    private final MealRepository mealRepository;
    private final ProjectRepository projectRepository;

    public MealUseCase(MealRepository mealRepository, ProjectRepository projectRepository) {
        this.mealRepository = mealRepository;
        this.projectRepository = projectRepository;
    }

    public MealResponseDTO create(MealRequestDTO dto) {
        Meal meal = MealMapper.toDomain(dto, findProject(dto.projectId()));
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

    public List<MealResponseDTO> findByProjectId(UUID projectId) {
        Project project = EntityLookup.require(projectId, projectRepository::findById,
                ResourceNotFoundException::project);
        return mealRepository.findByProject(project).stream()
                .map(MealMapper::toResponse)
                .toList();
    }

    public MealResponseDTO update(UUID id, MealRequestDTO dto) {
        Meal updated = MealMapper.toDomain(findMeal(id), dto, findProject(dto.projectId()));
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

    private Project findProject(UUID projectId) {
        return EntityLookup.optional(projectId, projectRepository::findById, ResourceNotFoundException::project);
    }
}
