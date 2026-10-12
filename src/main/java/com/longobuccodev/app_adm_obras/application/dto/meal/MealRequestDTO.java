package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A obra da refeicao e a obra do restaurante; nao ha campo de obra nem de endereco. */
public record MealRequestDTO(
        UUID restaurantId,
        BigDecimal price,
        Boolean isBilled,
        MealType mealType,
        Integer quantity,
        LocalDate date
) {
}
