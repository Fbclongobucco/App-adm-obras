package com.longobuccodev.app_adm_obras.application.dto.closing;

import com.longobuccodev.app_adm_obras.core.domain.DayClosing;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dia do fechamento. {@code partialNoService}: no restaurante, parte das refeicoes sem fornecimento.
 * {@code adjustmentIds}: ajustes por data que definem o dia.
 */
public record DayClosingDTO(
        LocalDate date,
        DayOfWeek weekday,
        DayClosing.Status status,
        boolean partialNoService,
        int lunchMeals,
        int dinnerMeals,
        int deliveries,
        int sodas,
        BigDecimal price,
        List<UUID> adjustmentIds
) {
}
