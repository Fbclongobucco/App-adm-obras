package com.longobuccodev.app_adm_obras.application.dto;

import com.longobuccodev.app_adm_obras.core.domain.Employee.Role;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record EmployeeResponseDTO(
        UUID id,
        String name,
        String email,
        String cpf,
        String phone,
        AddressResponseDTO address,
        LocalDate birthDate,
        CostCenterResponseDTO costCenter,
        Role role,
        Set<ProjectSummaryDTO> projects
) {
}
