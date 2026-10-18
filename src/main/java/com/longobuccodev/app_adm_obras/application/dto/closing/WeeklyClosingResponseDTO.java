package com.longobuccodev.app_adm_obras.application.dto.closing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fechamento semanal: semanas da obra, semana selecionada ({@code selected} = inicio dela; {@code null} se a
 * obra nao tem semanas), os 7 dias de segunda a domingo e os totais.
 */
public record WeeklyClosingResponseDTO(
        List<ClosingWeekDTO> weeks,
        LocalDate selected,
        List<DayClosingDTO> days,
        Totals totals
) {

    /** {@code serviceDays}: dias com fornecimento (normal, alterado ou dia extra). */
    public record Totals(int serviceDays, int lunchMeals, int dinnerMeals, int deliveries, int sodas,
                         BigDecimal price) {
    }
}
