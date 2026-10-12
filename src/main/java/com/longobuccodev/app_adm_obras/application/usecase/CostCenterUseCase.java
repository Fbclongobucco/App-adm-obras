package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.CostCenterMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class CostCenterUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public CostCenterUseCase(CostCenterRepository costCenterRepository, EmployeeRepository employeeRepository,
                             ProjectRepository projectRepository, Clock clock) {
        this.costCenterRepository = costCenterRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    public CostCenterResponseDTO create(CostCenterRequestDTO dto) {
        CostCenter costCenter = CostCenterMapper.toDomain(dto);
        costCenter.markCreated(clock.instant());
        return CostCenterMapper.toResponse(costCenterRepository.save(costCenter));
    }

    public CostCenterResponseDTO findById(UUID id) {
        return CostCenterMapper.toResponse(findCostCenter(id));
    }

    public List<CostCenterResponseDTO> findAll() {
        return costCenterRepository.findAll().stream()
                .map(CostCenterMapper::toResponse)
                .toList();
    }

    public PageResponseDTO<CostCenterResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(costCenterRepository.findAll(request), CostCenterMapper::toResponse);
    }

    public CostCenterResponseDTO update(UUID id, CostCenterRequestDTO dto) {
        CostCenter existing = findCostCenter(id);
        CostCenter updated = CostCenterMapper.toDomain(id, dto);
        updated.touchFrom(existing, clock.instant());
        costCenterRepository.update(updated);
        return CostCenterMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        CostCenter costCenter = findCostCenter(id);
        Conflicts.requireNone(employeeRepository.findByCostCenter(costCenter),
                () -> ConflictException.inUse("cost_center", "employees"));
        Conflicts.requireNone(projectRepository.findByCostCenter(costCenter),
                () -> ConflictException.inUse("cost_center", "projects"));
        costCenterRepository.deleteById(id);
    }

    private CostCenter findCostCenter(UUID id) {
        return EntityLookup.require(id, costCenterRepository::findById, ResourceNotFoundException::costCenter);
    }
}
