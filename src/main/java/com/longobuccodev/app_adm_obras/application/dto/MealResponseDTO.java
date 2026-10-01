package com.longobuccodev.app_adm_obras.application.dto;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MealResponseDTO(
        UUID id,
        String restaurantName,
        BigDecimal price,
        Boolean isBilled,
        AddressResponseDTO address,
        ProjectSummaryDTO project,
        MealType mealType,
        Integer quantity,
        LocalDate date,
        BigDecimal totalPrice
) {
}
