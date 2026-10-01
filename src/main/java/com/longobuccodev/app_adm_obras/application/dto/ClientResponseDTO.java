package com.longobuccodev.app_adm_obras.application.dto;

import java.util.Set;
import java.util.UUID;

public record ClientResponseDTO(
        UUID id,
        String name,
        String email,
        String phone,
        AddressResponseDTO address,
        Set<ProjectSummaryDTO> projects
) {
}
