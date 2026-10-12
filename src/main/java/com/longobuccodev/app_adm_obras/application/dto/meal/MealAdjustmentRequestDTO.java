package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;

import java.time.LocalDate;

/**
 * Ajuste por data da refeicao. {@code EXTRA_DAY}: sabado ou domingo, com as quantidades das refeicoes
 * servidas (0 a 10000, ao menos uma maior que zero). {@code NO_SERVICE}: dia util, sem quantidades.
 * {@code note}: opcional, ate 100 caracteres.
 */
public record MealAdjustmentRequestDTO(
        LocalDate date,
        MealAdjustment.Type type,
        Integer lunchQuantity,
        Integer dinnerQuantity,
        String note
) {
}
