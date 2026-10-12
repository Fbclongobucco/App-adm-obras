package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.meal.MealProjectionDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;

import java.util.List;
import java.util.UUID;

public final class MealMapper {

    private MealMapper() {
    }

    public static Meal toDomain(MealRequestDTO dto, Restaurant restaurant) {
        return build(null, dto, restaurant);
    }

    /** Refeicao alterada com os ajustes por data da existente, para a gravacao nao apaga-los. */
    public static Meal toDomain(Meal existing, MealRequestDTO dto, Restaurant restaurant) {
        Meal updated = build(existing.getId(), dto, restaurant);
        existing.getAdjustments().forEach(updated::restoreAdjustment);
        return updated;
    }

    public static MealSummaryDTO toSummary(Meal meal) {
        if (meal == null) {
            return null;
        }
        return new MealSummaryDTO(
                meal.getId(),
                RestaurantMapper.toSummary(meal.getRestaurant()),
                meal.getMealType(),
                meal.getBilled(),
                meal.getLunchQuantity(),
                meal.getLunchPrice(),
                meal.getDinnerQuantity(),
                meal.getDinnerPrice(),
                meal.getSodaPrice(),
                meal.getDeliveriesPerDay(),
                meal.getMealsDailyPrice(),
                meal.getDeliveryDailyPrice(),
                meal.getSodaDailyPrice(),
                meal.getDailyPrice(),
                meal.getWorkingDays(),
                meal.getServiceDays(),
                meal.getNoServiceDays(),
                meal.getExtraDays(),
                meal.getExtraDaysPrice(),
                meal.getTotalPrice(),
                toProjections(meal),
                meal.getCreatedAt(),
                meal.getUpdatedAt()
        );
    }

    public static MealResponseDTO toResponse(Meal meal) {
        return new MealResponseDTO(
                meal.getId(),
                RestaurantMapper.toSummary(meal.getRestaurant()),
                meal.getMealType(),
                meal.getBilled(),
                meal.getLunchQuantity(),
                meal.getLunchPrice(),
                meal.getDinnerQuantity(),
                meal.getDinnerPrice(),
                meal.getSodaPrice(),
                meal.getDeliveriesPerDay(),
                meal.getMealsDailyPrice(),
                meal.getDeliveryDailyPrice(),
                meal.getSodaDailyPrice(),
                meal.getDailyPrice(),
                meal.getWorkingDays(),
                meal.getServiceDays(),
                meal.getNoServiceDays(),
                meal.getExtraDays(),
                meal.getExtraDaysPrice(),
                meal.getTotalPrice(),
                toProjections(meal),
                meal.getCreatedAt(),
                meal.getUpdatedAt(),
                ProjectMapper.toSummary(meal.getProject()),
                MealAdjustmentMapper.toResponses(meal)
        );
    }

    private static List<MealProjectionDTO> toProjections(Meal meal) {
        return meal.getProjections().stream()
                .map(projection -> new MealProjectionDTO(projection.period(), projection.workingDays(),
                        projection.lunchMeals(), projection.dinnerMeals(), projection.deliveries(),
                        projection.sodas(), projection.price()))
                .toList();
    }

    private static Meal build(UUID id, MealRequestDTO dto, Restaurant restaurant) {
        return new Meal(id, restaurant, dto.mealType(), dto.isBilled(), dto.lunchQuantity(), dto.lunchPrice(),
                dto.dinnerQuantity(), dto.dinnerPrice());
    }
}
