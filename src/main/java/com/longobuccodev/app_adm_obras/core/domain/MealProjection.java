package com.longobuccodev.app_adm_obras.core.domain;

import java.math.BigDecimal;

/**
 * Quantidades e valor de uma refeicao num periodo de fechamento: valores diarios x dias uteis do periodo.
 *
 * @param lunchMeals  marmitas de almoco (0 se a refeicao nao serve almoco)
 * @param dinnerMeals marmitas de jantar (0 se a refeicao nao serve jantar)
 * @param deliveries  entregas (uma por refeicao servida por dia)
 * @param sodas       refrigerantes (um por entrega; 0 se a refeicao nao tem refrigerante)
 * @param price       custo por dia (marmitas, entrega e refrigerante) x dias uteis
 */
public record MealProjection(
        BillingPeriod period,
        int workingDays,
        int lunchMeals,
        int dinnerMeals,
        int deliveries,
        int sodas,
        BigDecimal price
) {
}
