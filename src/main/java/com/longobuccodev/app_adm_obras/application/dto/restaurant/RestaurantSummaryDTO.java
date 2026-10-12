package com.longobuccodev.app_adm_obras.application.dto.restaurant;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;

import java.time.Instant;
import java.util.UUID;

public record RestaurantSummaryDTO(
        UUID id,
        String name,
        String phone,
        AddressResponseDTO address,
        Instant createdAt,
        Instant updatedAt
) {
}
