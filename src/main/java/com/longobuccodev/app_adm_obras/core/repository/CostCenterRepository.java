package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;

import java.util.List;
import java.util.UUID;

public interface CostCenterRepository {

    CostCenter save(CostCenter costCenter);
    CostCenter findById(UUID id);
    CostCenter findByName(String name);
    List<CostCenter> findAll();
    default Page<CostCenter> findAll(PageRequest request) {
        return findAll(request, SearchTerm.NONE);
    }

    /** Pagina filtrada pelo termo nos campos pesquisaveis do recurso; {@link SearchTerm#NONE} nao filtra. */
    Page<CostCenter> findAll(PageRequest request, SearchTerm term);
    void delete(CostCenter costCenter);
    void deleteById(UUID id);
    void update(CostCenter costCenter);
}
