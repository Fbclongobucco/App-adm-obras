package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;

import java.util.List;

public final class ProjectEntityMapper {

    private ProjectEntityMapper() {
    }

    /** @param meals refeicoes da obra; cada uma passa a referenciar a obra montada aqui */
    public static Project toDomain(ProjectEntity entity, List<MealEntity> meals) {
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
        if (entity.getRestaurants() != null) {
            entity.getRestaurants().forEach(restaurantEntity ->
                    project.addRestaurant(RestaurantEntityMapper.toDomain(restaurantEntity)));
        }
        if (meals != null) {
            meals.forEach(mealEntity -> project.addMeal(MealEntityMapper.toDomain(mealEntity)));
        }
        return AuditMapping.restore(project, entity);
    }

    public static Project toDomainShallow(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuditMapping.restore(new Project(entity.getId(), entity.getOs(), entity.getDescription(),
                CostCenterEntityMapper.toDomain(entity.getCostCenter()),
                entity.getStartDate(), entity.getEndDate(),
                ClientEntityMapper.toDomain(entity.getClient()),
                entity.getIsCompleted()), entity);
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
        AuditMapping.copy(domain, entity);
    }
}
