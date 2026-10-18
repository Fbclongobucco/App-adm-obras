package com.longobuccodev.app_adm_obras.application.dto.closing;

import com.longobuccodev.app_adm_obras.core.domain.DayClosing;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dia do fechamento. {@code partialNoService}: no restaurante, parte das refeicoes sem fornecimento.
 * {@code deliveries}: entregas do almoco mais as do jantar. {@code adjustmentIds}: ajustes por data que definem
 * o dia; {@code note}: observacao do ajuste. {@code meals}: no restaurante, os numeros de cada refeicao no dia;
 * vazio no fechamento da refeicao.
 */
public record DayClosingDTO(
        LocalDate date,
        DayOfWeek weekday,
        DayClosing.Status status,
        boolean partialNoService,
        int lunchMeals,
        int dinnerMeals,
        int lunchDeliveries,
        int dinnerDeliveries,
        int deliveries,
        int sodas,
        BigDecimal price,
        List<UUID> adjustmentIds,
        String note,
        List<MealDay> meals
) {

    /** Numeros de uma refeicao do restaurante no dia. */
    public record MealDay(UUID mealId, DayClosing.Status status, int lunchMeals, int dinnerMeals,
                          int lunchDeliveries, int dinnerDeliveries, int sodas, BigDecimal price) {
    }
}
