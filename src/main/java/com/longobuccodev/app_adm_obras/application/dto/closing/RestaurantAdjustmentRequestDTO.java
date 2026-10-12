package com.longobuccodev.app_adm_obras.application.dto.closing;

import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ajuste por data em todas as refeicoes do restaurante. {@code NO_SERVICE}: em todas, sem quantidades.
 * {@code EXTRA_DAY}: so nas refeicoes de {@code quantities} com alguma quantidade maior que zero.
 */
public record RestaurantAdjustmentRequestDTO(
        LocalDate date,
        MealAdjustment.Type type,
        String note,
        List<MealQuantity> quantities
) {

    public record MealQuantity(UUID mealId, Integer lunchQuantity, Integer dinnerQuantity) {
    }
}
