package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.EmployeeEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.CostCenterEntity;
import com.longobuccodev.app_adm_obras.infra.entities.EmployeeEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.CostCenterJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.EmployeeJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.ProjectJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    /** Campos pesquisaveis da listagem (busca q). */
    private static final List<String> SEARCH_TEXT = List.of("name", "email");
    private static final List<String> SEARCH_DIGITS = List.of("cpf");

    private final EmployeeJpaRepository jpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    private final CostCenterJpaRepository costCenterJpaRepository;

    public EmployeeRepositoryAdapter(EmployeeJpaRepository jpaRepository,
                                     ProjectJpaRepository projectJpaRepository,
                                     CostCenterJpaRepository costCenterJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.projectJpaRepository = projectJpaRepository;
        this.costCenterJpaRepository = costCenterJpaRepository;
    }

    @Override
    @Transactional
    public Employee save(Employee employee) {
        return toDomain(jpaRepository.save(EmployeeEntityMapper.toEntity(employee, costCenterOf(employee))));
    }

    @Override
    @Transactional(readOnly = true)
    public Employee findById(UUID id) {
        return toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAllById(Collection<UUID> ids) {
        return toDomainAll(jpaRepository.findAllById(ids));
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
        return toDomainAll(jpaRepository.findByCostCenterId(costCenter.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByClient(com.longobuccodev.app_adm_obras.core.domain.Client client) {
        return toDomainAll(jpaRepository.findByClientId(client.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByProject(com.longobuccodev.app_adm_obras.core.domain.Project project) {
        return toDomainAll(jpaRepository.findByProjectId(project.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return toDomainAll(jpaRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findAll(PageRequest request, SearchTerm term) {
        Pageable pageable = SpringPageSupport.toPageable(request);
        return SpringPageSupport.toDomainAll(term.isNone()
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findAll(SearchSpecification.of(term, SEARCH_TEXT, SEARCH_DIGITS), pageable), this::toDomainAll);
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
                .ifPresent(entity -> EmployeeEntityMapper.apply(employee, entity, costCenterOf(employee)));
    }

    private CostCenterEntity costCenterOf(Employee employee) {
        return employee.getCostCenter() == null
                ? null
                : costCenterJpaRepository.getReferenceById(employee.getCostCenter().getId());
    }

    private Employee toDomain(EmployeeEntity entity) {
        if (entity == null) {
            return null;
        }
        return toDomainAll(List.of(entity)).getFirst();
    }

    /** Obras de todos os colaboradores da lista em uma consulta, agrupadas por colaborador. */
    private List<Employee> toDomainAll(List<EmployeeEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ProjectEntity>> projects = projectJpaRepository
                .findWithEmployeeIdIn(entities.stream().map(EmployeeEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(row -> (UUID) row[0],
                        Collectors.mapping(row -> (ProjectEntity) row[1], Collectors.toList())));
        return entities.stream()
                .map(entity -> EmployeeEntityMapper.toDomain(entity, projects.getOrDefault(entity.getId(), List.of())))
                .toList();
    }
}
