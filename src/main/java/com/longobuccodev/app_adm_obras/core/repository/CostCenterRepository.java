package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.CostCenter;

import java.util.List;
import java.util.UUID;

public interface CostCenterRepository {

    CostCenter save(CostCenter costCenter);
    CostCenter findById(UUID id);
    CostCenter findByName(String name);
    List<CostCenter> findAll();
    void delete(CostCenter costCenter);
    void deleteById(UUID id);
    void update(CostCenter costCenter);
}
