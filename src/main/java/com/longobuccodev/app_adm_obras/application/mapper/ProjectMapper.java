package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.ProjectRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.ProjectResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.ProjectSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static Project toDomain(ProjectRequestDTO dto, CostCenter costCenter, Client client,
                                   Set<Accommodation> accommodations, Set<Employee> employees,
                                   Meal lunch, Meal dinner) {
        return build(null, dto, costCenter, client, accommodations, employees, lunch, dinner);
    }

    public static Project toDomain(Project existing, ProjectRequestDTO dto, CostCenter costCenter, Client client,
                                   Set<Accommodation> accommodations, Set<Employee> employees,
                                   Meal lunch, Meal dinner) {
        return build(existing.getId(), dto, costCenter, client, accommodations, employees, lunch, dinner);
    }

    public static ProjectSummaryDTO toSummary(Project project) {
        if (project == null) {
            return null;
        }
        return new ProjectSummaryDTO(
                project.getId(),
                project.getOs(),
                project.getDescription(),
                CostCenterMapper.toResponse(project.getCostCenter()),
                project.getStartDate(),
                project.getEndDate(),
                ClientMapper.toSummary(project.getClient()),
                project.getTotalPrice(),
                project.getIsCompleted()
        );
    }

    public static ProjectResponseDTO toResponse(Project project) {
        return new ProjectResponseDTO(
                project.getId(),
                project.getOs(),
                project.getDescription(),
                CostCenterMapper.toResponse(project.getCostCenter()),
                project.getStartDate(),
                project.getEndDate(),
                ClientMapper.toSummary(project.getClient()),
                project.getAccommodations().stream()
                        .map(AccommodationMapper::toSummary)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                project.getEmployees().stream()
                        .map(EmployeeMapper::toSummary)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                MealMapper.toSummary(project.getLunch()),
                MealMapper.toSummary(project.getDinner()),
                project.getTotalPrice(),
                project.getIsCompleted()
        );
    }

    private static Project build(UUID id, ProjectRequestDTO dto, CostCenter costCenter, Client client,
                                 Set<Accommodation> accommodations, Set<Employee> employees,
                                 Meal lunch, Meal dinner) {
        Project project = new Project(id, dto.os(), dto.description(), costCenter, dto.startDate(),
                dto.endDate(), client, dto.isCompleted());
        if (accommodations != null) {
            accommodations.forEach(project::addAccommodation);
        }
        if (employees != null) {
            employees.forEach(project::addEmployee);
        }
        project.setLunch(lunch);
        project.setDinner(dinner);
        return project;
    }
}
