package com.longobuccodev.app_adm_obras.application.dto.restaurant;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RestaurantSummaryDTO(
        UUID id,
        String name,
        String phone,
        BigDecimal lunchDeliveryFee,
        BigDecimal dinnerDeliveryFee,
        BigDecimal sodaPrice,
        AddressResponseDTO address,
        Instant createdAt,
        Instant updatedAt
) {
}
