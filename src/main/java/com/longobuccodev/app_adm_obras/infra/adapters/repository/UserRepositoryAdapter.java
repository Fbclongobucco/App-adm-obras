package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.UserEntityMapper;
import com.longobuccodev.app_adm_obras.infra.repositories.UserJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

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
    public Page<User> findAll(PageRequest request) {
        return SpringPageSupport.toDomain(
                jpaRepository.findAll(SpringPageSupport.toPageable(request)), UserEntityMapper::toDomain);
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
