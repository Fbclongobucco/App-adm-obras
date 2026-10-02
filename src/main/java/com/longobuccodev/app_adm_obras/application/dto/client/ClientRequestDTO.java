package com.longobuccodev.app_adm_obras.application.dto.client;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;

public record ClientRequestDTO(
        String name,
        String email,
        String phone,
        AddressRequestDTO address
) {
}
