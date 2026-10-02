package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.EmployeeEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.EmployeeEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.EmployeeJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.ProjectJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final EmployeeJpaRepository jpaRepository;
    private final ProjectJpaRepository projectJpaRepository;

    public EmployeeRepositoryAdapter(EmployeeJpaRepository jpaRepository,
                                     ProjectJpaRepository projectJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.projectJpaRepository = projectJpaRepository;
    }

    @Override
    @Transactional
    public Employee save(Employee employee) {
        return toDomain(jpaRepository.save(EmployeeEntityMapper.toEntity(employee)));
    }

    @Override
    @Transactional(readOnly = true)
    public Employee findById(UUID id) {
        return toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Employee findByName(String name) {
        return toDomain(jpaRepository.findFirstByName(name).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Employee findByCpf(String cpf) {
        return toDomain(jpaRepository.findFirstByCpf(cpf).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Employee findByEmail(String email) {
        return toDomain(jpaRepository.findFirstByEmail(email).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByCostCenter(com.longobuccodev.app_adm_obras.core.domain.CostCenter costCenter) {
        return jpaRepository.findByCostCenterId(costCenter.getId()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByClient(com.longobuccodev.app_adm_obras.core.domain.Client client) {
        return jpaRepository.findByClientId(client.getId()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByProject(com.longobuccodev.app_adm_obras.core.domain.Project project) {
        return jpaRepository.findByProjectId(project.getId()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findAll(PageRequest request) {
        return SpringPageSupport.toDomain(
                jpaRepository.findAll(SpringPageSupport.toPageable(request)), this::toDomain);
    }

    @Override
    @Transactional
    public void delete(Employee employee) {
        jpaRepository.deleteById(employee.getId());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(Employee employee) {
        jpaRepository.findById(employee.getId())
                .ifPresent(entity -> EmployeeEntityMapper.apply(employee, entity));
    }

    private Employee toDomain(EmployeeEntity entity) {
        if (entity == null) {
            return null;
        }
        List<ProjectEntity> projects = projectJpaRepository.findByEmployeeId(entity.getId());
        return EmployeeEntityMapper.toDomain(entity, projects);
    }
}
