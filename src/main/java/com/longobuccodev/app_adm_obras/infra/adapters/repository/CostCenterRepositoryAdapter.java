package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.CostCenterEntityMapper;
import com.longobuccodev.app_adm_obras.infra.repositories.CostCenterJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class CostCenterRepositoryAdapter implements CostCenterRepository {

    private final CostCenterJpaRepository jpaRepository;

    public CostCenterRepositoryAdapter(CostCenterJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public CostCenter save(CostCenter costCenter) {
        return CostCenterEntityMapper.toDomain(jpaRepository.save(CostCenterEntityMapper.toEntity(costCenter)));
    }

    @Override
    @Transactional(readOnly = true)
    public CostCenter findById(UUID id) {
        return CostCenterEntityMapper.toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public CostCenter findByName(String name) {
        return CostCenterEntityMapper.toDomain(jpaRepository.findFirstByName(name).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenter> findAll() {
        return jpaRepository.findAll().stream().map(CostCenterEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CostCenter> findAll(PageRequest request) {
        return SpringPageSupport.toDomain(
                jpaRepository.findAll(SpringPageSupport.toPageable(request)), CostCenterEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(CostCenter costCenter) {
        jpaRepository.delete(CostCenterEntityMapper.toEntity(costCenter));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(CostCenter costCenter) {
        jpaRepository.findById(costCenter.getId())
                .ifPresent(entity -> CostCenterEntityMapper.apply(costCenter, entity));
    }
}
