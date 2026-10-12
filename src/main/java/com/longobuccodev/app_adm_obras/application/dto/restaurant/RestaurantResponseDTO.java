package com.longobuccodev.app_adm_obras.application.dto.restaurant;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;

import java.time.Instant;
import java.util.UUID;

public record RestaurantResponseDTO(
        UUID id,
        String name,
        String phone,
        AddressResponseDTO address,
        ProjectSummaryDTO project,
        Instant createdAt,
        Instant updatedAt
) {
}
