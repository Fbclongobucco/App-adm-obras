package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealJpaRepository extends JpaRepository<MealEntity, UUID> {

    Optional<MealEntity> findFirstByProjectIdAndMealType(UUID projectId, MealType mealType);

    @Query("""
            SELECT meal FROM MealEntity meal
            WHERE meal.project.id = :projectId
            """)
    List<MealEntity> findByProjectId(@Param("projectId") UUID projectId);

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

    List<MealEntity> findByDate(LocalDate date);

    List<MealEntity> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<MealEntity> findByMealType(MealType mealType);
}
