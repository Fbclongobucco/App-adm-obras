package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.MealEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.RestaurantEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.MealJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.RestaurantJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class MealRepositoryAdapter implements MealRepository {

    /** Campos pesquisaveis da listagem (busca q). */
    private static final List<String> SEARCH_TEXT = List.of("restaurant.name", "project.os");
    private static final List<String> SEARCH_DIGITS = List.of();

    private final MealJpaRepository jpaRepository;
    private final RestaurantJpaRepository restaurantJpaRepository;

    public MealRepositoryAdapter(MealJpaRepository jpaRepository, RestaurantJpaRepository restaurantJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.restaurantJpaRepository = restaurantJpaRepository;
    }

    @Override
    @Transactional
    public Meal save(Meal meal) {
        return MealEntityMapper.toDomain(jpaRepository.save(MealEntityMapper.toEntity(meal, restaurantOf(meal))));
    }

    @Override
    @Transactional(readOnly = true)
    public Meal findById(UUID id) {
        return MealEntityMapper.toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meal> findAll() {
        return jpaRepository.findAll().stream().map(MealEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Meal> findAll(PageRequest request, SearchTerm term) {
        Pageable pageable = SpringPageSupport.toPageable(request);
        return SpringPageSupport.toDomain(term.isNone()
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findAll(SearchSpecification.of(term, SEARCH_TEXT, SEARCH_DIGITS), pageable), MealEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meal> findByEmployee(Employee employee) {
        return jpaRepository.findByEmployeeId(employee.getId()).stream().map(MealEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meal> findByCostCenter(CostCenter costCenter) {
        return jpaRepository.findByCostCenterId(costCenter.getId()).stream()
                .map(MealEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meal> findByClient(Client client) {
        return jpaRepository.findByClientId(client.getId()).stream().map(MealEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meal> findByProject(Project project) {
        return jpaRepository.findByProjectId(project.getId()).stream().map(MealEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meal> findByRestaurantIn(Collection<Restaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = restaurants.stream().map(Restaurant::getId).distinct().toList();
        return jpaRepository.findByRestaurantIdIn(ids).stream().map(MealEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meal> findByMealType(Meal.MealType mealType) {
        return jpaRepository.findByMealType(mealType).stream().map(MealEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(Meal meal) {
        jpaRepository.deleteById(meal.getId());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(Meal meal) {
        jpaRepository.findById(meal.getId())
                .ifPresent(entity -> MealEntityMapper.apply(meal, entity, restaurantOf(meal)));
    }

    @Override
    @Transactional
    public void updateAll(List<Meal> meals) {
        meals.forEach(this::update);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByRestaurantId(UUID restaurantId) {
        return jpaRepository.existsByRestaurantId(restaurantId);
    }

    /** Entidade gerenciada: a obra da refeicao e copiada dela. */
    private RestaurantEntity restaurantOf(Meal meal) {
        return restaurantJpaRepository.findById(meal.getRestaurant().getId()).orElseThrow();
    }
}
