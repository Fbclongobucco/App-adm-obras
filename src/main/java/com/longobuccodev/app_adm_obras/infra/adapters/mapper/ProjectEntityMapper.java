package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;

public final class ProjectEntityMapper {

    private ProjectEntityMapper() {
    }

    public static Project toDomain(ProjectEntity entity, MealEntity lunch, MealEntity dinner) {
        if (entity == null) {
            return null;
        }
        Project project = new Project(entity.getId(), entity.getOs(), entity.getDescription(),
                CostCenterEntityMapper.toDomain(entity.getCostCenter()),
                entity.getStartDate(), entity.getEndDate(),
                ClientEntityMapper.toDomain(entity.getClient()),
                entity.getIsCompleted());
        if (entity.getAccommodations() != null) {
            entity.getAccommodations().forEach(accommodationEntity ->
                    project.addAccommodation(AccommodationEntityMapper.toDomain(accommodationEntity)));
        }
        if (entity.getEmployees() != null) {
            entity.getEmployees().forEach(employeeEntity ->
                    project.addEmployee(EmployeeEntityMapper.toDomain(employeeEntity)));
        }
        linkMeal(project, MealEntityMapper.toDomain(lunch, false), true);
        linkMeal(project, MealEntityMapper.toDomain(dinner, false), false);
        return project;
    }

    public static Project toDomainShallow(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Project(entity.getId(), entity.getOs(), entity.getDescription(),
                CostCenterEntityMapper.toDomain(entity.getCostCenter()),
                entity.getStartDate(), entity.getEndDate(),
                ClientEntityMapper.toDomain(entity.getClient()),
                entity.getIsCompleted());
    }

    public static ProjectEntity toEntity(Project domain) {
        if (domain == null) {
            return null;
        }
        ProjectEntity entity = ProjectEntity.create();
        entity.setId(domain.getId());
        apply(domain, entity);
        return entity;
    }

    public static void apply(Project domain, ProjectEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setOs(domain.getOs());
        entity.setDescription(domain.getDescription());
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setIsCompleted(domain.getIsCompleted());
        entity.setTotalPrice(domain.getTotalPrice());
    }

    private static void linkMeal(Project project, Meal meal, boolean lunch) {
        if (meal == null) {
            return;
        }
        if (lunch) {
            project.setLunch(meal);
        } else {
            project.setDinner(meal);
        }
        meal.setProject(project);
    }
}
