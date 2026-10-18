package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.ProjectEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationEntity;
import com.longobuccodev.app_adm_obras.infra.entities.EmployeeEntity;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.AccommodationJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.ClientJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.CostCenterJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.EmployeeJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.MealJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.ProjectJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ProjectRepositoryAdapter implements ProjectRepository {

    /** Campos pesquisaveis da listagem (busca q). */
    private static final List<String> SEARCH_TEXT = List.of("os", "description", "client.name");
    private static final List<String> SEARCH_DIGITS = List.of();

    // Campo de ordenacao do dominio -> propriedade de ProjectEntity
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "updatedAt", "updatedAt",
            "createdAt", "createdAt",
            "os", "os",
            "startDate", "startDate");

    private final ProjectJpaRepository jpaRepository;
    private final CostCenterJpaRepository costCenterJpaRepository;
    private final ClientJpaRepository clientJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;
    private final MealJpaRepository mealJpaRepository;
    private final AccommodationJpaRepository accommodationJpaRepository;

    public ProjectRepositoryAdapter(ProjectJpaRepository jpaRepository,
                                    CostCenterJpaRepository costCenterJpaRepository,
                                    ClientJpaRepository clientJpaRepository,
                                    EmployeeJpaRepository employeeJpaRepository,
                                    MealJpaRepository mealJpaRepository,
                                    AccommodationJpaRepository accommodationJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.costCenterJpaRepository = costCenterJpaRepository;
        this.clientJpaRepository = clientJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
        this.mealJpaRepository = mealJpaRepository;
        this.accommodationJpaRepository = accommodationJpaRepository;
    }

    @Override
    @Transactional
    public Project save(Project project) {
        ProjectEntity entity = ProjectEntityMapper.toEntity(project);
        applyRelations(project, entity);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Project findById(UUID id) {
        return toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Project findByOs(String os) {
        return toDomain(jpaRepository.findFirstByOs(os).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return toDomainAll(jpaRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findAll(PageRequest request, SearchTerm term) {
        Pageable pageable = SpringPageSupport.toPageable(request, SORT_PROPERTIES);
        return SpringPageSupport.toDomainAll(term.isNone()
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findAll(SearchSpecification.of(term, SEARCH_TEXT, SEARCH_DIGITS), pageable), this::toDomainAll);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByClient(Client client) {
        return toDomainAll(jpaRepository.findByClientId(client.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByCostCenter(CostCenter costCenter) {
        return toDomainAll(jpaRepository.findByCostCenterId(costCenter.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByEmployee(Employee employee) {
        return toDomainAll(jpaRepository.findByEmployeeId(employee.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByStartDate(LocalDate startDate) {
        return toDomainAll(jpaRepository.findByStartDate(startDate));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByEndDate(LocalDate endDate) {
        return toDomainAll(jpaRepository.findByEndDate(endDate));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return toDomainAll(jpaRepository.findByStartDateBetween(startDate, endDate));
    }

    @Override
    @Transactional
    public void delete(Project project) {
        jpaRepository.deleteById(project.getId());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(Project project) {
        ProjectEntity entity = jpaRepository.findById(project.getId()).orElse(null);
        if (entity == null) {
            return;
        }
        ProjectEntityMapper.apply(project, entity);
        applyRelations(project, entity);
    }

    // Uma consulta por lista de vinculos, e nao um findById por item
    private void applyRelations(Project project, ProjectEntity entity) {
        if (project.getCostCenter() != null) {
            entity.setCostCenter(costCenterJpaRepository.findById(project.getCostCenter().getId()).orElse(null));
        }
        if (project.getClient() != null) {
            entity.setClient(clientJpaRepository.findById(project.getClient().getId()).orElse(null));
        }
        if (entity.getEmployees() == null) {
            entity.setEmployees(new LinkedHashSet<>());
        }
        entity.getEmployees().clear();
        entity.getEmployees().addAll(BatchLookup.inOrder(project.getEmployees().stream().map(Employee::getId).toList(),
                employeeJpaRepository::findAllById, EmployeeEntity::getId));
        if (entity.getAccommodations() == null) {
            entity.setAccommodations(new LinkedHashSet<>());
        }
        entity.getAccommodations().clear();
        for (var accommodationEntity : BatchLookup.inOrder(project.getAccommodations().stream().map(Accommodation::getId).toList(),
                accommodationJpaRepository::findAllById, AccommodationEntity::getId)) {
            accommodationEntity.setProject(entity);
            entity.getAccommodations().add(accommodationEntity);
        }
    }

    private Project toDomain(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return toDomainAll(List.of(entity)).getFirst();
    }

    /** Refeicoes de todas as obras da lista em uma consulta, agrupadas por obra. */
    private List<Project> toDomainAll(List<ProjectEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<MealEntity>> meals = mealJpaRepository
                .findByProjectIdIn(entities.stream().map(ProjectEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(meal -> meal.getProject().getId()));
        return entities.stream()
                .map(entity -> ProjectEntityMapper.toDomain(entity, meals.getOrDefault(entity.getId(), List.of())))
                .toList();
    }
}
