package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.EmployeeRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.EmployeeResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.EmployeeSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static Employee toDomain(EmployeeRequestDTO dto, CostCenter costCenter) {
        return new Employee(null, dto.name(), dto.email(), dto.cpf(), dto.phone(),
                AddressMapper.toDomain(dto.address()), dto.birthDate(), costCenter, null, dto.role());
    }

    public static Employee toDomain(Employee existing, EmployeeRequestDTO dto, CostCenter costCenter) {
        return new Employee(existing.getId(), dto.name(), dto.email(), dto.cpf(), dto.phone(),
                AddressMapper.toDomain(AddressMapper.idOf(existing.getAddress()), dto.address()),
                dto.birthDate(), costCenter, existing.getProjects(), dto.role());
    }

    public static EmployeeSummaryDTO toSummary(Employee employee) {
        if (employee == null) {
            return null;
        }
        return new EmployeeSummaryDTO(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getCpf(),
                employee.getPhone(),
                AddressMapper.toResponse(employee.getAddress()),
                employee.getBirthDate(),
                CostCenterMapper.toResponse(employee.getCostCenter()),
                employee.getRole()
        );
    }

    public static EmployeeResponseDTO toResponse(Employee employee) {
        return new EmployeeResponseDTO(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getCpf(),
                employee.getPhone(),
                AddressMapper.toResponse(employee.getAddress()),
                employee.getBirthDate(),
                CostCenterMapper.toResponse(employee.getCostCenter()),
                employee.getRole(),
                employee.getProjects().stream()
                        .map(ProjectMapper::toSummary)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }
}
