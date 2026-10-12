package com.longobuccodev.app_adm_obras.application.dto.restaurant;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;

import java.util.UUID;

public record RestaurantRequestDTO(
        String name,
        String phone,
        AddressRequestDTO address,
        UUID projectId
) {
}
