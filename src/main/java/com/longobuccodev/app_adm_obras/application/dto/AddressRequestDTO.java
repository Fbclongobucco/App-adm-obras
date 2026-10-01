package com.longobuccodev.app_adm_obras.application.dto;

public record AddressRequestDTO(
        String street,
        String number,
        String city,
        String state,
        String country,
        String neighborhood,
        String zipCode
) {
}
