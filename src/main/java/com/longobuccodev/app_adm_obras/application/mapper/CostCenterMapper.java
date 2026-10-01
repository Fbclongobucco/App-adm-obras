package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.CostCenterRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;

import java.util.UUID;

public final class CostCenterMapper {

    private CostCenterMapper() {
    }

    public static CostCenter toDomain(CostCenterRequestDTO dto) {
        return toDomain(null, dto);
    }

    public static CostCenter toDomain(UUID id, CostCenterRequestDTO dto) {
        return new CostCenter(id, dto.name(), dto.cnpj());
    }

    public static CostCenterResponseDTO toResponse(CostCenter costCenter) {
        if (costCenter == null) {
            return null;
        }
        return new CostCenterResponseDTO(costCenter.getId(), costCenter.getName(), costCenter.getCnpj());
    }
}
