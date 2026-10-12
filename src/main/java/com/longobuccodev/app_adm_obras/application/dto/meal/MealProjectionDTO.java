package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.core.domain.BillingPeriod;

import java.math.BigDecimal;

/** Quantidades e valor da refeicao num periodo de fechamento (DAY = 1, WEEK = 5, FORTNIGHT = 10, MONTH = 22). */
public record MealProjectionDTO(
        BillingPeriod period,
        Integer workingDays,
        Integer lunchMeals,
        Integer dinnerMeals,
        Integer deliveries,
        Integer sodas,
        BigDecimal price
) {
}
