package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Refeicao sem a obra, usada dentro da resposta da obra e do restaurante. {@code dailyPrice} =
 * {@code mealsDailyPrice + deliveryDailyPrice + sodaDailyPrice}. {@code sodaPrice} e o do restaurante.
 * {@code serviceDays = workingDays - noServiceDays}; {@code totalPrice = dailyPrice x serviceDays +
 * extraDaysPrice} (ajustes no periodo atual da obra; null sem termino).
 */
public record MealSummaryDTO(
        UUID id,
        RestaurantSummaryDTO restaurant,
        MealType mealType,
        Boolean isBilled,
        Integer lunchQuantity,
        BigDecimal lunchPrice,
        Integer dinnerQuantity,
        BigDecimal dinnerPrice,
        BigDecimal sodaPrice,
        Integer deliveriesPerDay,
        BigDecimal mealsDailyPrice,
        BigDecimal deliveryDailyPrice,
        BigDecimal sodaDailyPrice,
        BigDecimal dailyPrice,
        Integer workingDays,
        Integer serviceDays,
        Integer noServiceDays,
        Integer extraDays,
        BigDecimal extraDaysPrice,
        BigDecimal totalPrice,
        List<MealProjectionDTO> projections,
        Instant createdAt,
        Instant updatedAt
) {
}
