package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ajuste por data com o valor calculado pela refeicao atual. {@code lunchDeliveries}, {@code dinnerDeliveries}
 * e {@code sodas} sao os efetivos (informados ou padrao); {@code deliveries} e a soma. Numeros e
 * {@code price} sao zero em {@code NO_SERVICE}. {@code inPeriod} falso: a data ficou fora do periodo
 * atual da obra e nao entra no total.
 */
public record MealAdjustmentResponseDTO(
        UUID id,
        LocalDate date,
        DayOfWeek weekday,
        MealAdjustment.Type type,
        Integer lunchQuantity,
        Integer dinnerQuantity,
        String note,
        Integer lunchDeliveries,
        Integer dinnerDeliveries,
        Integer deliveries,
        Integer sodas,
        BigDecimal price,
        Boolean inPeriod
) {
}
