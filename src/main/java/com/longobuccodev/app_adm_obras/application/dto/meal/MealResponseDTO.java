package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@link MealSummaryDTO} mais a obra da refeicao e os ajustes por data, ordenados por data. */
public record MealResponseDTO(
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
        Instant updatedAt,
        ProjectSummaryDTO project,
        List<MealAdjustmentResponseDTO> adjustments
) {
}
