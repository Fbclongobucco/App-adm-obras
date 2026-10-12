package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.UserEntityMapper;
import com.longobuccodev.app_adm_obras.infra.repositories.UserJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    /** Campos pesquisaveis da listagem (busca q). */
    private static final List<String> SEARCH_TEXT = List.of("name", "email");
    private static final List<String> SEARCH_DIGITS = List.of();

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public User save(User user) {
        return UserEntityMapper.toDomain(jpaRepository.save(UserEntityMapper.toEntity(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return UserEntityMapper.toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return UserEntityMapper.toDomain(jpaRepository.findFirstByEmailIgnoreCase(email).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(UserEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAll(PageRequest request, SearchTerm term) {
        Pageable pageable = SpringPageSupport.toPageable(request);
        return SpringPageSupport.toDomain(term.isNone()
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findAll(SearchSpecification.of(term, SEARCH_TEXT, SEARCH_DIGITS), pageable), UserEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(User user) {
        jpaRepository.delete(UserEntityMapper.toEntity(user));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(User user) {
        jpaRepository.findById(user.getId())
                .ifPresent(entity -> UserEntityMapper.apply(user, entity));
    }
}
