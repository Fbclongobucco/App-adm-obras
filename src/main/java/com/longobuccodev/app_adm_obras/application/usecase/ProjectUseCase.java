package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.application.mapper.ProjectMapper;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.exception.InvalidProjectException;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.core.repository.Sort;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProjectUseCase {

    private final ProjectRepository projectRepository;
    private final CostCenterRepository costCenterRepository;
    private final ClientRepository clientRepository;
    private final AccommodationRepository accommodationRepository;
    private final EmployeeRepository employeeRepository;
    private final MealRepository mealRepository;
    private final Clock clock;

    public ProjectUseCase(ProjectRepository projectRepository,
                          CostCenterRepository costCenterRepository,
                          ClientRepository clientRepository,
                          AccommodationRepository accommodationRepository,
                          EmployeeRepository employeeRepository,
                          MealRepository mealRepository,
                          Clock clock) {
        this.projectRepository = projectRepository;
        this.costCenterRepository = costCenterRepository;
        this.clientRepository = clientRepository;
        this.accommodationRepository = accommodationRepository;
        this.employeeRepository = employeeRepository;
        this.mealRepository = mealRepository;
        this.clock = clock;
    }

    public ProjectResponseDTO create(ProjectRequestDTO dto) {
        Project project = toDomain(null, dto);
        requireUniqueOs(project);
        project.markCreated(clock.instant());
        return ProjectMapper.toResponse(projectRepository.save(project));
    }

    public ProjectResponseDTO findById(UUID id) {
        return ProjectMapper.toResponse(findProject(id));
    }

    public List<ProjectResponseDTO> findAll() {
        return projectRepository.findAll().stream()
                .map(ProjectMapper::toResponse)
                .toList();
    }

    public PageResponseDTO<ProjectResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(projectRepository.findAll(request), ProjectMapper::toResponse);
    }

    /** Listagem ordenada; {@code sort} no formato campo,direcao, restrito a {@link ProjectRepository#SORT_FIELDS}. */
    public PageResponseDTO<ProjectResponseDTO> findAll(PageRequest request, String sort) {
        return findAll(request.withSort(Sort.parse(sort, ProjectRepository.SORT_FIELDS)));
    }

    public ProjectResponseDTO update(UUID id, ProjectRequestDTO dto) {
        Project existing = findProject(id);
        Project updated = toDomain(existing, dto);
        existing.getRestaurants().forEach(updated::addRestaurant);
        requireUniqueOs(updated);
        updated.touchFrom(existing, clock.instant());
        projectRepository.update(updated);
        return ProjectMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        Project project = findProject(id);
        Conflicts.requireNone(project.getRestaurants(),
                () -> ConflictException.inUse("project", "restaurants"));
        Conflicts.requireNone(mealRepository.findByProject(project),
                () -> ConflictException.inUse("project", "meals"));
        Conflicts.requireNone(accommodationRepository.findByProjectId(id),
                () -> ConflictException.inUse("project", "accommodations"));
        projectRepository.deleteById(id);
    }

    private void requireUniqueOs(Project project) {
        Conflicts.requireUnique(projectRepository.findByOs(project.getOs()), Project::getId, project.getId(),
                () -> ConflictException.duplicate("project", "os", project.getOs()));
    }

    private Project findProject(UUID id) {
        return EntityLookup.require(id, projectRepository::findById, ResourceNotFoundException::project);
    }

    /**
     * lunchId/dinnerId so confirmam uma refeicao que ja pertence a obra pelo restaurante; nao movem
     * refeicoes. Sem o id, vale a refeicao atual da obra.
     */
    private Meal ownMeal(Project existing, UUID mealId, Meal current) {
        if (mealId == null) {
            return current;
        }
        Meal meal = EntityLookup.require(mealId, mealRepository::findById, ResourceNotFoundException::meal);
        if (existing == null || meal.getProject() == null || !meal.getProject().getId().equals(existing.getId())) {
            throw InvalidProjectException.mealFromAnotherProject(mealId);
        }
        return meal;
    }

    private Project toDomain(Project existing, ProjectRequestDTO dto) {
        CostCenter costCenter = EntityLookup.optional(dto.costCenterId(), costCenterRepository::findById,
                ResourceNotFoundException::costCenter);
        Client client = EntityLookup.optional(dto.clientId(), clientRepository::findById,
                ResourceNotFoundException::client);
        Set<Accommodation> accommodations = EntityLookup.all(dto.accommodationIds(),
                accommodationRepository::findById, ResourceNotFoundException::accommodation);
        Set<Employee> employees = EntityLookup.all(dto.employeeIds(), employeeRepository::findById,
                ResourceNotFoundException::employee);
        Meal lunch = ownMeal(existing, dto.lunchId(), existing == null ? null : existing.getLunch());
        Meal dinner = ownMeal(existing, dto.dinnerId(), existing == null ? null : existing.getDinner());

        if (existing == null) {
            return ProjectMapper.toDomain(dto, costCenter, client, accommodations, employees, lunch, dinner);
        }
        return ProjectMapper.toDomain(existing, dto, costCenter, client, accommodations, employees, lunch, dinner);
    }
}
