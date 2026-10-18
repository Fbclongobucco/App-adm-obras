package com.longobuccodev.app_adm_obras.application.dto.restaurant;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Restaurante com a obra e as refeicoes que ele serve. */
public record RestaurantResponseDTO(
        UUID id,
        String name,
        String phone,
        BigDecimal lunchDeliveryFee,
        BigDecimal dinnerDeliveryFee,
        BigDecimal sodaPrice,
        AddressResponseDTO address,
        ProjectSummaryDTO project,
        List<MealSummaryDTO> meals,
        Instant createdAt,
        Instant updatedAt
) {
}
