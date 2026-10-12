package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.meal.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;

public final class MealMapper {

    private MealMapper() {
    }

    public static Meal toDomain(MealRequestDTO dto, Restaurant restaurant) {
        return new Meal(null, restaurant, dto.price(), dto.isBilled(), dto.mealType(), dto.quantity(), dto.date());
    }

    public static Meal toDomain(Meal existing, MealRequestDTO dto, Restaurant restaurant) {
        return new Meal(existing.getId(), restaurant, dto.price(), dto.isBilled(), dto.mealType(), dto.quantity(),
                dto.date());
    }

    public static MealSummaryDTO toSummary(Meal meal) {
        if (meal == null) {
            return null;
        }
        return new MealSummaryDTO(
                meal.getId(),
                RestaurantMapper.toSummary(meal.getRestaurant()),
                meal.getPrice(),
                meal.getBilled(),
                meal.getMealType(),
                meal.getQuantity(),
                meal.getDate(),
                meal.getTotalPrice(),
                meal.getCreatedAt(),
                meal.getUpdatedAt()
        );
    }

    public static MealResponseDTO toResponse(Meal meal) {
        return new MealResponseDTO(
                meal.getId(),
                RestaurantMapper.toSummary(meal.getRestaurant()),
                meal.getPrice(),
                meal.getBilled(),
                ProjectMapper.toSummary(meal.getProject()),
                meal.getMealType(),
                meal.getQuantity(),
                meal.getDate(),
                meal.getTotalPrice(),
                meal.getCreatedAt(),
                meal.getUpdatedAt()
        );
    }
}
