package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.closing.RestaurantAdjustmentRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.closing.WeeklyClosingResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.MealAdjustmentMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.application.mapper.RestaurantMapper;
import com.longobuccodev.app_adm_obras.application.mapper.WeeklyClosingMapper;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.exception.InvalidMealAdjustmentException;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.core.repository.RestaurantRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final ProjectRepository projectRepository;
    private final MealRepository mealRepository;
    private final Clock clock;

    public RestaurantUseCase(RestaurantRepository restaurantRepository, ProjectRepository projectRepository,
                             MealRepository mealRepository, Clock clock) {
        this.restaurantRepository = restaurantRepository;
        this.projectRepository = projectRepository;
        this.mealRepository = mealRepository;
        this.clock = clock;
    }

    public RestaurantResponseDTO create(RestaurantRequestDTO dto) {
        Restaurant restaurant = RestaurantMapper.toDomain(dto, findProject(dto.projectId()));
        restaurant.markCreated(clock.instant());
        // Restaurante novo ainda nao serve refeicoes
        return RestaurantMapper.toResponse(restaurantRepository.save(restaurant), List.of());
    }

    public RestaurantResponseDTO findById(UUID id) {
        return withMeals(findRestaurant(id));
    }

    public PageResponseDTO<RestaurantResponseDTO> findAll(PageRequest request) {
        Page<Restaurant> page = restaurantRepository.findAll(request);
        return PageMapper.toResponse(page, withMeals(page.content()));
    }

    /** Listagem filtrada pelo termo de busca; vazio ou nulo nao filtra. */
    public PageResponseDTO<RestaurantResponseDTO> findAll(PageRequest request, String query) {
        Page<Restaurant> page = restaurantRepository.findAll(request, SearchTerm.of(query));
        return PageMapper.toResponse(page, withMeals(page.content()));
    }

    public List<RestaurantResponseDTO> findByProjectId(UUID projectId) {
        EntityLookup.require(projectId, projectRepository::findById, ResourceNotFoundException::project);
        List<Restaurant> restaurants = restaurantRepository.findByProjectId(projectId);
        return restaurants.stream().map(withMeals(restaurants)).toList();
    }

    /** Fechamento semanal somando as refeicoes do restaurante. */
    public WeeklyClosingResponseDTO weeklyClosing(UUID id, LocalDate week) {
        Restaurant restaurant = findRestaurant(id);
        return WeeklyClosingMapper.toResponse(restaurant.getProject(), mealsOf(restaurant), week,
                LocalDate.now(clock));
    }

    /**
     * Ajuste por data em todas as refeicoes do restaurante: sem fornecimento em todas; dia extra so nas
     * refeicoes com alguma quantidade maior que zero. As regras de cada refeicao rodam antes de gravar e a
     * gravacao e unica: qualquer erro deixa todas as refeicoes como estavam.
     */
    public List<MealAdjustmentResponseDTO> addAdjustment(UUID id, RestaurantAdjustmentRequestDTO dto) {
        List<Meal> meals = mealsOf(findRestaurant(id));
        Map<UUID, RestaurantAdjustmentRequestDTO.MealQuantity> quantities = new HashMap<>();
        for (RestaurantAdjustmentRequestDTO.MealQuantity quantity
                : dto.quantities() == null ? List.<RestaurantAdjustmentRequestDTO.MealQuantity>of() : dto.quantities()) {
            if (meals.stream().noneMatch(meal -> meal.getId().equals(quantity.mealId()))) {
                throw ResourceNotFoundException.meal(quantity.mealId());
            }
            quantities.put(quantity.mealId(), quantity);
        }
        Instant now = clock.instant();
        List<Meal> changed = new ArrayList<>();
        List<MealAdjustmentResponseDTO> added = new ArrayList<>();
        for (Meal meal : meals) {
            MealAdjustment adjustment;
            if (dto.type() == MealAdjustment.Type.EXTRA_DAY) {
                RestaurantAdjustmentRequestDTO.MealQuantity quantity = quantities.get(meal.getId());
                if (quantity == null || !positive(quantity.lunchQuantity()) && !positive(quantity.dinnerQuantity())) {
                    continue;
                }
                adjustment = new MealAdjustment(null, dto.date(), dto.type(),
                        orZero(quantity.lunchQuantity()), orZero(quantity.dinnerQuantity()), dto.note());
            } else {
                adjustment = new MealAdjustment(null, dto.date(), dto.type(), null, null, dto.note());
            }
            MealAdjustment accepted = meal.addAdjustment(adjustment);
            accepted.markCreated(now);
            meal.touch(now);
            changed.add(meal);
            added.add(MealAdjustmentMapper.toResponse(accepted, meal));
        }
        if (changed.isEmpty()) {
            throw InvalidMealAdjustmentException.invalidQuantity(
                    "Restaurant adjustment must apply to at least one meal");
        }
        mealRepository.updateAll(changed);
        return added;
    }

    /** Remove os ajustes da data em todas as refeicoes do restaurante. */
    public void removeAdjustments(UUID id, LocalDate date) {
        Instant now = clock.instant();
        List<Meal> changed = new ArrayList<>();
        for (Meal meal : mealsOf(findRestaurant(id))) {
            List<UUID> onDate = meal.getAdjustments().stream()
                    .filter(adjustment -> adjustment.getDate().equals(date))
                    .map(MealAdjustment::getId).toList();
            if (!onDate.isEmpty()) {
                onDate.forEach(meal::removeAdjustment);
                meal.touch(now);
                changed.add(meal);
            }
        }
        if (changed.isEmpty()) {
            throw ResourceNotFoundException.restaurantAdjustment(date);
        }
        mealRepository.updateAll(changed);
    }

    public RestaurantResponseDTO update(UUID id, RestaurantRequestDTO dto) {
        Restaurant existing = findRestaurant(id);
        Restaurant updated = RestaurantMapper.toDomain(existing, dto, findProject(dto.projectId()));
        // As refeicoes herdam a obra do restaurante; mover o restaurante as levaria junto
        boolean projectChanged = !existing.getProject().getId().equals(updated.getProject().getId());
        Conflicts.requireFalse(projectChanged && mealRepository.existsByRestaurantId(id),
                () -> ConflictException.inUse("restaurant", "meals"));
        updated.touchFrom(existing, clock.instant());
        restaurantRepository.update(updated);
        return withMeals(updated);
    }

    private List<Meal> mealsOf(Restaurant restaurant) {
        return mealRepository.findByRestaurantIn(List.of(restaurant));
    }

    private static boolean positive(Integer quantity) {
        return quantity != null && quantity > 0;
    }

    private static Integer orZero(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    public void delete(UUID id) {
        findRestaurant(id);
        Conflicts.requireFalse(mealRepository.existsByRestaurantId(id),
                () -> ConflictException.inUse("restaurant", "meals"));
        restaurantRepository.deleteById(id);
    }

    private RestaurantResponseDTO withMeals(Restaurant restaurant) {
        return withMeals(List.of(restaurant)).apply(restaurant);
    }

    /** Refeicoes de todos os restaurantes numa consulta, agrupadas por restaurante. */
    private Function<Restaurant, RestaurantResponseDTO> withMeals(List<Restaurant> restaurants) {
        Map<UUID, List<Meal>> mealsByRestaurant = mealRepository.findByRestaurantIn(restaurants).stream()
                .collect(Collectors.groupingBy(meal -> meal.getRestaurant().getId()));
        return restaurant -> RestaurantMapper.toResponse(restaurant,
                mealsByRestaurant.getOrDefault(restaurant.getId(), List.of()));
    }

    private Restaurant findRestaurant(UUID id) {
        return EntityLookup.require(id, restaurantRepository::findById, ResourceNotFoundException::restaurant);
    }

    private Project findProject(UUID projectId) {
        return EntityLookup.optional(projectId, projectRepository::findById, ResourceNotFoundException::project);
    }
}
