package com.longobuccodev.app_adm_obras.application.dto;

import java.util.UUID;

public record AddressResponseDTO(
        UUID id,
        String street,
        String number,
        String city,
        String state,
        String country,
        String neighborhood,
        String zipCode
) {
}
