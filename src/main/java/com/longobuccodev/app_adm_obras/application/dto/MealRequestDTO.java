package com.longobuccodev.app_adm_obras.application.dto;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MealRequestDTO(
        String restaurantName,
        BigDecimal price,
        Boolean isBilled,
        AddressRequestDTO address,
        UUID projectId,
        MealType mealType,
        Integer quantity,
        LocalDate date
) {
}
