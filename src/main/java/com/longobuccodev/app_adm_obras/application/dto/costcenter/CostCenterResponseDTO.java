package com.longobuccodev.app_adm_obras.application.dto.costcenter;

import java.util.UUID;

public record CostCenterResponseDTO(
        UUID id,
        String name,
        String cnpj
) {
}
