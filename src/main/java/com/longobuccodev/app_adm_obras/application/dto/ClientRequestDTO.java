package com.longobuccodev.app_adm_obras.application.dto;

public record ClientRequestDTO(
        String name,
        String email,
        String phone,
        AddressRequestDTO address
) {
}
