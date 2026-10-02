package com.longobuccodev.app_adm_obras.application.dto.employee;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Employee.Role;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeSummaryDTO(
        UUID id,
        String name,
        String email,
        String cpf,
        String phone,
        AddressResponseDTO address,
        LocalDate birthDate,
        CostCenterResponseDTO costCenter,
        Role role
) {
}
