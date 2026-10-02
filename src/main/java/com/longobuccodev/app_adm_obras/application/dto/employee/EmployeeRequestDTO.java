package com.longobuccodev.app_adm_obras.application.dto.employee;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.core.domain.Employee.Role;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRequestDTO(
        String name,
        String email,
        String cpf,
        String phone,
        AddressRequestDTO address,
        LocalDate birthDate,
        UUID costCenterId,
        Role role
) {
}
