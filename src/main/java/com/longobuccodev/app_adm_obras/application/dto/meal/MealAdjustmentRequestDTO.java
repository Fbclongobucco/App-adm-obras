package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;

import java.time.LocalDate;

/**
 * Ajuste por data da refeicao. {@code EXTRA_DAY} (sabado ou domingo) e {@code CHANGED} (dia util): com as
 * quantidades das refeicoes servidas (0 a 10000, ao menos uma maior que zero) e, opcionais (vazio = padrao),
 * entregas do almoco e do jantar (0 a 100) e refrigerantes (0 a 10000). {@code NO_SERVICE}: dia util, sem
 * numeros. {@code date} e ignorada no PUT por data. {@code note}: opcional, ate 100 caracteres.
 */
public record MealAdjustmentRequestDTO(
        LocalDate date,
        MealAdjustment.Type type,
        Integer lunchQuantity,
        Integer dinnerQuantity,
        Integer lunchDeliveries,
        Integer dinnerDeliveries,
        Integer sodas,
        String note
) {
}
