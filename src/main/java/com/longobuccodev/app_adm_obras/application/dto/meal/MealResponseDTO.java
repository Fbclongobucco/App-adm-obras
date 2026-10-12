package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;

import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MealResponseDTO(
        UUID id,
        RestaurantSummaryDTO restaurant,
        BigDecimal price,
        Boolean isBilled,
        ProjectSummaryDTO project,
        MealType mealType,
        Integer quantity,
        LocalDate date,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant updatedAt
) {
}
