package com.longobuccodev.app_adm_obras.application.dto.meal;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A obra da refeicao e a obra do restaurante. Quantidade e preco sao obrigatorios para cada
 * refeicao servida pelo tipo; os da refeicao nao servida sao ignorados. O refrigerante vem do
 * restaurante; {@code sodaPrice} enviado aqui e ignorado.
 */
public record MealRequestDTO(
        UUID restaurantId,
        MealType mealType,
        Boolean isBilled,
        Integer lunchQuantity,
        BigDecimal lunchPrice,
        Integer dinnerQuantity,
        BigDecimal dinnerPrice
) {
}
