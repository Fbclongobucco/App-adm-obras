package com.longobuccodev.app_adm_obras.application.dto;

import java.util.UUID;

public record CostCenterResponseDTO(
        UUID id,
        String name,
        String cnpj
) {
}
