package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.infra.entities.CostCenterEntity;

public final class CostCenterEntityMapper {

    private CostCenterEntityMapper() {
    }

    public static CostCenter toDomain(CostCenterEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CostCenter(entity.getId(), entity.getName(), entity.getCnpj());
    }

    public static CostCenterEntity toEntity(CostCenter domain) {
        if (domain == null) {
            return null;
        }
        CostCenterEntity entity = CostCenterEntity.create();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setCnpj(domain.getCnpj());
        return entity;
    }

    public static void apply(CostCenter domain, CostCenterEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setName(domain.getName());
        entity.setCnpj(domain.getCnpj());
    }
}
