package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.ProjectEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.AccommodationJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.ClientJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.CostCenterJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.EmployeeJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.MealJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.ProjectJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Repository
public class ProjectRepositoryAdapter implements ProjectRepository {

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
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findAll(PageRequest request) {
        return SpringPageSupport.toDomain(
                jpaRepository.findAll(SpringPageSupport.toPageable(request)), this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByClient(Client client) {
        return jpaRepository.findByClientId(client.getId()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByCostCenter(CostCenter costCenter) {
        return jpaRepository.findByCostCenterId(costCenter.getId()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByEmployee(Employee employee) {
        return jpaRepository.findByEmployeeId(employee.getId()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByStartDate(LocalDate startDate) {
        return jpaRepository.findByStartDate(startDate).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByEndDate(LocalDate endDate) {
        return jpaRepository.findByEndDate(endDate).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByStartDateBetween(startDate, endDate).stream().map(this::toDomain).toList();
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
        for (Employee employee : project.getEmployees()) {
            employeeJpaRepository.findById(employee.getId()).ifPresent(entity.getEmployees()::add);
        }
        if (entity.getAccommodations() == null) {
            entity.setAccommodations(new LinkedHashSet<>());
        }
        entity.getAccommodations().clear();
        for (var accommodation : project.getAccommodations()) {
            accommodationJpaRepository.findById(accommodation.getId()).ifPresent(accommodationEntity -> {
                accommodationEntity.setProject(entity);
                entity.getAccommodations().add(accommodationEntity);
            });
        }
        assignMeals(project, entity);
    }

    private void assignMeals(Project project, ProjectEntity entity) {
        mealJpaRepository.findByProjectId(entity.getId()).forEach(mealEntity -> mealEntity.setProject(null));
        assignMeal(project.getLunch(), entity, Meal.MealType.LUNCH);
        assignMeal(project.getDinner(), entity, Meal.MealType.DINNER);
    }

    private void assignMeal(Meal meal, ProjectEntity entity, Meal.MealType mealType) {
        if (meal == null) {
            return;
        }
        mealJpaRepository.findById(meal.getId()).ifPresent(mealEntity -> {
            mealEntity.setProject(entity);
            mealEntity.setMealType(mealType);
        });
    }

    private Project toDomain(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProjectEntityMapper.toDomain(entity,
                mealJpaRepository.findFirstByProjectIdAndMealType(entity.getId(), Meal.MealType.LUNCH).orElse(null),
                mealJpaRepository.findFirstByProjectIdAndMealType(entity.getId(), Meal.MealType.DINNER).orElse(null));
    }
}
