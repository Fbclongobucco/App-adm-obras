package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.MealSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;

public final class MealMapper {

    private MealMapper() {
    }

    public static Meal toDomain(MealRequestDTO dto, Project project) {
        return new Meal(null, dto.restaurantName(), dto.price(), dto.isBilled(),
                AddressMapper.toDomain(dto.address()), project, dto.mealType(), dto.quantity(), dto.date());
    }

    public static Meal toDomain(Meal existing, MealRequestDTO dto, Project project) {
        return new Meal(existing.getId(), dto.restaurantName(), dto.price(), dto.isBilled(),
                AddressMapper.toDomain(AddressMapper.idOf(existing.getAddress()), dto.address()),
                project, dto.mealType(), dto.quantity(), dto.date());
    }

    public static MealSummaryDTO toSummary(Meal meal) {
        if (meal == null) {
            return null;
        }
        return new MealSummaryDTO(
                meal.getId(),
                meal.getRestaurantName(),
                meal.getPrice(),
                meal.getBilled(),
                AddressMapper.toResponse(meal.getAddress()),
                meal.getMealType(),
                meal.getQuantity(),
                meal.getDate(),
                meal.getTotalPrice()
        );
    }

    public static MealResponseDTO toResponse(Meal meal) {
        return new MealResponseDTO(
                meal.getId(),
                meal.getRestaurantName(),
                meal.getPrice(),
                meal.getBilled(),
                AddressMapper.toResponse(meal.getAddress()),
                ProjectMapper.toSummary(meal.getProject()),
                meal.getMealType(),
                meal.getQuantity(),
                meal.getDate(),
                meal.getTotalPrice()
        );
    }
}
