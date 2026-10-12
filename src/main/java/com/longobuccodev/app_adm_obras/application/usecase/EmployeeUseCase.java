package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.EmployeeMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class EmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final CostCenterRepository costCenterRepository;

    private final ProjectRepository projectRepository;
    private final AccommodationRepository accommodationRepository;
    private final Clock clock;

    public EmployeeUseCase(EmployeeRepository employeeRepository, CostCenterRepository costCenterRepository,
                           ProjectRepository projectRepository, AccommodationRepository accommodationRepository,
                           Clock clock) {
        this.employeeRepository = employeeRepository;
        this.costCenterRepository = costCenterRepository;
        this.projectRepository = projectRepository;
        this.accommodationRepository = accommodationRepository;
        this.clock = clock;
    }

    public EmployeeResponseDTO create(EmployeeRequestDTO dto) {
        Employee employee = EmployeeMapper.toDomain(dto, findCostCenter(dto.costCenterId()));
        requireUniqueDocuments(employee);
        employee.markCreated(clock.instant());
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

    /** Listagem filtrada pelo termo de busca; vazio ou nulo nao filtra. */
    public PageResponseDTO<EmployeeResponseDTO> findAll(PageRequest request, String query) {
        return PageMapper.toResponse(employeeRepository.findAll(request, SearchTerm.of(query)), EmployeeMapper::toResponse);
    }

    public List<EmployeeResponseDTO> findByCostCenterId(UUID costCenterId) {
        CostCenter costCenter = EntityLookup.require(costCenterId, costCenterRepository::findById,
                ResourceNotFoundException::costCenter);
        return employeeRepository.findByCostCenter(costCenter).stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    public EmployeeResponseDTO update(UUID id, EmployeeRequestDTO dto) {
        Employee existing = findEmployee(id);
        Employee updated = EmployeeMapper.toDomain(existing, dto, findCostCenter(dto.costCenterId()));
        requireUniqueDocuments(updated);
        updated.touchFrom(existing, clock.instant());
        employeeRepository.update(updated);
        return EmployeeMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        Employee employee = findEmployee(id);
        Conflicts.requireNone(projectRepository.findByEmployee(employee),
                () -> ConflictException.inUse("employee", "projects"));
        Conflicts.requireFalse(accommodationRepository.existsByEmployeeId(id),
                () -> ConflictException.inUse("employee", "accommodations"));
        employeeRepository.deleteById(id);
    }

    private void requireUniqueDocuments(Employee employee) {
        Conflicts.requireUnique(employeeRepository.findByCpf(employee.getCpf()), Employee::getId, employee.getId(),
                () -> ConflictException.duplicate("employee", "cpf", employee.getCpf()));
        // E-mail e opcional: so o informado precisa ser unico
        if (employee.getEmail() != null) {
            Conflicts.requireUnique(employeeRepository.findByEmail(employee.getEmail()), Employee::getId,
                    employee.getId(), () -> ConflictException.duplicate("employee", "email", employee.getEmail()));
        }
    }

    private Employee findEmployee(UUID id) {
        return EntityLookup.require(id, employeeRepository::findById, ResourceNotFoundException::employee);
    }

    private CostCenter findCostCenter(UUID costCenterId) {
        return EntityLookup.optional(costCenterId, costCenterRepository::findById,
                ResourceNotFoundException::costCenter);
    }
}
