package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.ProjectRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.ProjectResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.application.mapper.ProjectMapper;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;

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

    public ProjectUseCase(ProjectRepository projectRepository,
                          CostCenterRepository costCenterRepository,
                          ClientRepository clientRepository,
                          AccommodationRepository accommodationRepository,
                          EmployeeRepository employeeRepository,
                          MealRepository mealRepository) {
        this.projectRepository = projectRepository;
        this.costCenterRepository = costCenterRepository;
        this.clientRepository = clientRepository;
        this.accommodationRepository = accommodationRepository;
        this.employeeRepository = employeeRepository;
        this.mealRepository = mealRepository;
    }

    public ProjectResponseDTO create(ProjectRequestDTO dto) {
        return ProjectMapper.toResponse(projectRepository.save(toDomain(null, dto)));
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

    public ProjectResponseDTO update(UUID id, ProjectRequestDTO dto) {
        Project updated = toDomain(findProject(id), dto);
        projectRepository.update(updated);
        return ProjectMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findProject(id);
        projectRepository.deleteById(id);
    }

    private Project findProject(UUID id) {
        return EntityLookup.require(id, projectRepository::findById, ResourceNotFoundException::project);
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
        Meal lunch = EntityLookup.optional(dto.lunchId(), mealRepository::findById, ResourceNotFoundException::meal);
        Meal dinner = EntityLookup.optional(dto.dinnerId(), mealRepository::findById, ResourceNotFoundException::meal);

        if (existing == null) {
            return ProjectMapper.toDomain(dto, costCenter, client, accommodations, employees, lunch, dinner);
        }
        return ProjectMapper.toDomain(existing, dto, costCenter, client, accommodations, employees, lunch, dinner);
    }
}
