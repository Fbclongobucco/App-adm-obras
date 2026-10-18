package com.longobuccodev.app_adm_obras.application.dto.closing;

import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;

import java.util.List;
import java.util.UUID;

/**
 * Numeros de um dia em todas as refeicoes do restaurante. {@code EXTRA_DAY}: so as refeicoes com alguma
 * quantidade maior que zero recebem o ajuste; as demais ficam sem ajuste na data. {@code CHANGED}: refeicao
 * com todas as quantidades zero fica sem fornecimento; refeicao com os numeros do padrao fica sem ajuste.
 * Refeicao ausente de {@code meals} nao muda. Entregas e refrigerantes vazios usam o padrao.
 */
public record RestaurantDayRequestDTO(
        MealAdjustment.Type type,
        String note,
        List<MealDay> meals
) {

    public record MealDay(UUID mealId, Integer lunchQuantity, Integer dinnerQuantity, Integer lunchDeliveries,
                          Integer dinnerDeliveries, Integer sodas) {
    }
}
