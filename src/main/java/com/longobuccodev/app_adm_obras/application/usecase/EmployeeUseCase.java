package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.EmployeeMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;

import java.util.List;
import java.util.UUID;

public class EmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final CostCenterRepository costCenterRepository;

    public EmployeeUseCase(EmployeeRepository employeeRepository, CostCenterRepository costCenterRepository) {
        this.employeeRepository = employeeRepository;
        this.costCenterRepository = costCenterRepository;
    }

    public EmployeeResponseDTO create(EmployeeRequestDTO dto) {
        Employee employee = EmployeeMapper.toDomain(dto, findCostCenter(dto.costCenterId()));
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponseDTO findById(UUID id) {
        return EmployeeMapper.toResponse(findEmployee(id));
    }

    public List<EmployeeResponseDTO> findAll() {
        return employeeRepository.findAll().stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    public PageResponseDTO<EmployeeResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(employeeRepository.findAll(request), EmployeeMapper::toResponse);
    }

    public List<EmployeeResponseDTO> findByCostCenterId(UUID costCenterId) {
        CostCenter costCenter = EntityLookup.require(costCenterId, costCenterRepository::findById,
                ResourceNotFoundException::costCenter);
        return employeeRepository.findByCostCenter(costCenter).stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    public EmployeeResponseDTO update(UUID id, EmployeeRequestDTO dto) {
        Employee updated = EmployeeMapper.toDomain(findEmployee(id), dto, findCostCenter(dto.costCenterId()));
        employeeRepository.update(updated);
        return EmployeeMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findEmployee(id);
        employeeRepository.deleteById(id);
    }

    private Employee findEmployee(UUID id) {
        return EntityLookup.require(id, employeeRepository::findById, ResourceNotFoundException::employee);
    }

    private CostCenter findCostCenter(UUID costCenterId) {
        return EntityLookup.optional(costCenterId, costCenterRepository::findById,
                ResourceNotFoundException::costCenter);
    }
}
