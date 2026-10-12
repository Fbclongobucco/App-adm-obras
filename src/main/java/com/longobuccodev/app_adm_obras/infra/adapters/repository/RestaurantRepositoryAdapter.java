package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.RestaurantRepository;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.RestaurantEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.ProjectJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.RestaurantJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class RestaurantRepositoryAdapter implements RestaurantRepository {

    /** Campos pesquisaveis da listagem (busca q). */
    private static final List<String> SEARCH_TEXT = List.of("name", "project.os", "address.city");
    private static final List<String> SEARCH_DIGITS = List.of();

    private final RestaurantJpaRepository jpaRepository;
    private final ProjectJpaRepository projectJpaRepository;

    public RestaurantRepositoryAdapter(RestaurantJpaRepository jpaRepository,
                                       ProjectJpaRepository projectJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.projectJpaRepository = projectJpaRepository;
    }

    @Override
    @Transactional
    public Restaurant save(Restaurant restaurant) {
        return RestaurantEntityMapper.toDomain(
                jpaRepository.save(RestaurantEntityMapper.toEntity(restaurant, projectOf(restaurant))));
    }

    @Override
    @Transactional(readOnly = true)
    public Restaurant findById(UUID id) {
        return RestaurantEntityMapper.toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Restaurant> findAll(PageRequest request, SearchTerm term) {
        Pageable pageable = SpringPageSupport.toPageable(request);
        return SpringPageSupport.toDomain(term.isNone()
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findAll(SearchSpecification.of(term, SEARCH_TEXT, SEARCH_DIGITS), pageable), RestaurantEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Restaurant> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectIdOrderByName(projectId).stream()
                .map(RestaurantEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void update(Restaurant restaurant) {
        jpaRepository.findById(restaurant.getId())
                .ifPresent(entity -> RestaurantEntityMapper.apply(restaurant, entity, projectOf(restaurant)));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private ProjectEntity projectOf(Restaurant restaurant) {
        return projectJpaRepository.getReferenceById(restaurant.getProject().getId());
    }
}
