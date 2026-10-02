package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.CostCenterRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.CostCenterMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;

import java.util.List;
import java.util.UUID;

public class CostCenterUseCase {

    private final CostCenterRepository costCenterRepository;

    public CostCenterUseCase(CostCenterRepository costCenterRepository) {
        this.costCenterRepository = costCenterRepository;
    }

    public CostCenterResponseDTO create(CostCenterRequestDTO dto) {
        return CostCenterMapper.toResponse(costCenterRepository.save(CostCenterMapper.toDomain(dto)));
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
        findCostCenter(id);
        CostCenter updated = CostCenterMapper.toDomain(id, dto);
        costCenterRepository.update(updated);
        return CostCenterMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findCostCenter(id);
        costCenterRepository.deleteById(id);
    }

    private CostCenter findCostCenter(UUID id) {
        return EntityLookup.require(id, costCenterRepository::findById, ResourceNotFoundException::costCenter);
    }
}
