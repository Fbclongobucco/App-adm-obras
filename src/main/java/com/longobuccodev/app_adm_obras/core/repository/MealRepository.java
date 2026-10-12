package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MealRepository {

    Meal save(Meal meal);
    List<Meal> findAll();
    default Page<Meal> findAll(PageRequest request) {
        return findAll(request, SearchTerm.NONE);
    }

    /** Pagina filtrada pelo termo nos campos pesquisaveis do recurso; {@link SearchTerm#NONE} nao filtra. */
    Page<Meal> findAll(PageRequest request, SearchTerm term);
    List<Meal> findByEmployee(Employee employee);
    List<Meal> findByCostCenter(CostCenter costCenter);
    List<Meal> findByClient(Client client);
    List<Meal> findByProject(Project project);

    /** Refeicoes de todos os restaurantes informados, numa consulta. */
    List<Meal> findByRestaurantIn(Collection<Restaurant> restaurants);

    boolean existsByRestaurantId(UUID restaurantId);
    List<Meal> findByMealType(Meal.MealType mealType);
    Meal findById(UUID id);
    void delete(Meal meal);
    void deleteById(UUID id);
    void update(Meal meal);

    /** Grava as refeicoes numa unica transacao: todas ou nenhuma. */
    void updateAll(List<Meal> meals);
}
