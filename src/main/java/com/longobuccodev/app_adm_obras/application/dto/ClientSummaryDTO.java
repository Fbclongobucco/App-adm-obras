package com.longobuccodev.app_adm_obras.application.dto;

import java.util.UUID;

public record ClientSummaryDTO(
        UUID id,
        String name,
        String email,
        String phone,
        AddressResponseDTO address
) {
}
