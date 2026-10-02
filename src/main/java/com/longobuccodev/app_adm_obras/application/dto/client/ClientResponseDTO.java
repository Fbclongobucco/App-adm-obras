package com.longobuccodev.app_adm_obras.application.dto.client;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;
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
