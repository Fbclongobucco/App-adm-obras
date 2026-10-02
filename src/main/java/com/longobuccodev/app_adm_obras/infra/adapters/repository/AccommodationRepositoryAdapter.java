package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.AccommodationEntityMapper;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.ProjectEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.AccommodationJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.EmployeeJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.ProjectJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Repository
public class AccommodationRepositoryAdapter implements AccommodationRepository {

    private final AccommodationJpaRepository jpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public AccommodationRepositoryAdapter(AccommodationJpaRepository jpaRepository,
                                          ProjectJpaRepository projectJpaRepository,
                                          EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.projectJpaRepository = projectJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    @Transactional
    public Accommodation save(Accommodation accommodation) {
        AccommodationEntity entity = AccommodationEntityMapper.toEntity(accommodation);
        if (accommodation.getProject() != null) {
            projectJpaRepository.findById(accommodation.getProject().getId())
                    .ifPresent(entity::setProject);
        }
        replaceEmployees(entity, accommodation);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Accommodation findById(UUID id) {
        return toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Accommodation findByHostName(String hostName) {
        return toDomain(jpaRepository.findFirstByHostName(hostName).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Accommodation> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Accommodation> findAll(PageRequest request) {
        return SpringPageSupport.toDomain(
                jpaRepository.findAll(SpringPageSupport.toPageable(request)), this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Accommodation> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(Accommodation accommodation) {
        jpaRepository.deleteById(accommodation.getId());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(UUID id, Accommodation accommodation) {
        jpaRepository.findById(id).ifPresent(entity -> {
            AccommodationEntityMapper.apply(accommodation, entity);
            if (accommodation.getProject() != null) {
                projectJpaRepository.findById(accommodation.getProject().getId())
                        .ifPresent(entity::setProject);
            }
            replaceEmployees(entity, accommodation);
        });
    }

    private void replaceEmployees(AccommodationEntity entity, Accommodation accommodation) {
        entity.setEmployees(new LinkedHashSet<>());
        for (var employee : accommodation.getEmployees()) {
            employeeJpaRepository.findById(employee.getId()).ifPresent(entity.getEmployees()::add);
        }
    }

    private Accommodation toDomain(AccommodationEntity entity) {
        if (entity == null) {
            return null;
        }
        Accommodation accommodation = AccommodationEntityMapper.toDomain(entity);
        if (entity.getProject() != null) {
            accommodation.setProject(ProjectEntityMapper.toDomainShallow(entity.getProject()));
        }
        return accommodation;
    }
}
