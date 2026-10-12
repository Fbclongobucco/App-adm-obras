package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;

import java.util.List;

public final class MealAdjustmentMapper {

    private MealAdjustmentMapper() {
    }

    public static MealAdjustment toDomain(MealAdjustmentRequestDTO dto) {
        return new MealAdjustment(null, dto.date(), dto.type(), dto.lunchQuantity(), dto.dinnerQuantity(),
                dto.note());
    }

    /** Valores calculados a partir da refeicao do ajuste. */
    public static MealAdjustmentResponseDTO toResponse(MealAdjustment adjustment, Meal meal) {
        return new MealAdjustmentResponseDTO(adjustment.getId(), adjustment.getDate(), adjustment.getWeekday(),
                adjustment.getType(), adjustment.getLunchQuantity(), adjustment.getDinnerQuantity(),
                adjustment.getNote(), adjustment.deliveries(meal), adjustment.sodas(meal), adjustment.price(meal),
                meal.isInPeriod(adjustment));
    }

    /** Ajustes da refeicao, ordenados por data. */
    public static List<MealAdjustmentResponseDTO> toResponses(Meal meal) {
        return meal.getAdjustments().stream().map(adjustment -> toResponse(adjustment, meal)).toList();
    }
}
