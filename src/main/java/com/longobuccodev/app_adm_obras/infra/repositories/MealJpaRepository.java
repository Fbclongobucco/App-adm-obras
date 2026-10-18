package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MealJpaRepository extends JpaRepository<MealEntity, UUID>,
        JpaSpecificationExecutor<MealEntity> {

    @Query("""
            SELECT meal FROM MealEntity meal
            WHERE meal.project.id = :projectId
            ORDER BY meal.createdAt, meal.id
            """)
    List<MealEntity> findByProjectId(@Param("projectId") UUID projectId);

    @Query("""
            SELECT meal FROM MealEntity meal
            WHERE meal.project.id IN :projectIds
            ORDER BY meal.createdAt, meal.id
            """)
    List<MealEntity> findByProjectIdIn(@Param("projectIds") Collection<UUID> projectIds);

    @Query("""
            SELECT meal FROM MealEntity meal
            WHERE meal.project.costCenter.id = :costCenterId
            """)
    List<MealEntity> findByCostCenterId(@Param("costCenterId") UUID costCenterId);

    @Query("""
            SELECT meal FROM MealEntity meal
            WHERE meal.project.client.id = :clientId
            """)
    List<MealEntity> findByClientId(@Param("clientId") UUID clientId);

    @Query("""
            SELECT DISTINCT meal FROM MealEntity meal
            JOIN meal.project project
            JOIN project.employees employee
            WHERE employee.id = :employeeId
            """)
    List<MealEntity> findByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("""
            SELECT meal FROM MealEntity meal
            WHERE meal.restaurant.id IN :restaurantIds
            ORDER BY meal.createdAt, meal.id
            """)
    List<MealEntity> findByRestaurantIdIn(@Param("restaurantIds") Collection<UUID> restaurantIds);

    List<MealEntity> findByMealType(MealType mealType);

    boolean existsByRestaurantId(UUID restaurantId);
}
