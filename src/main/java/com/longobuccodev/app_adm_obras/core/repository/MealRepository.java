package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MealRepository {

    Meal save(Meal meal);
    List<Meal> findAll();
    List<Meal> findByEmployee(Employee employee);
    List<Meal> findByCostCenter(CostCenter costCenter);
    List<Meal> findByClient(Client client);
    List<Meal> findByProject(Project project);
    List<Meal> findByDate(LocalDate date);
    List<Meal> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<Meal> findByMealType(Meal.MealType mealType);
    Meal findById(UUID id);
    void delete(Meal meal);
    void deleteById(UUID id);
    void update(Meal meal);
}
