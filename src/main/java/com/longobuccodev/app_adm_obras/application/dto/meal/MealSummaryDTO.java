package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MealSummaryDTO(
        UUID id,
        String restaurantName,
        BigDecimal price,
        Boolean isBilled,
        AddressResponseDTO address,
        MealType mealType,
        Integer quantity,
        LocalDate date,
        BigDecimal totalPrice
) {
}
