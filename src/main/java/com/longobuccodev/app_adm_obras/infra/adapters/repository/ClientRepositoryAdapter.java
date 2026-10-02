package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.ClientEntityMapper;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.ProjectEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.ClientEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.ClientJpaRepository;
import com.longobuccodev.app_adm_obras.infra.repositories.ProjectJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class ClientRepositoryAdapter implements ClientRepository {

    private final ClientJpaRepository jpaRepository;
    private final ProjectJpaRepository projectJpaRepository;

    public ClientRepositoryAdapter(ClientJpaRepository jpaRepository, ProjectJpaRepository projectJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.projectJpaRepository = projectJpaRepository;
    }

    @Override
    @Transactional
    public Client save(Client client) {
        return toDomain(jpaRepository.save(ClientEntityMapper.toEntity(client)));
    }

    @Override
    @Transactional(readOnly = true)
    public Client findById(UUID id) {
        return toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Client findByProjectId(UUID projectId) {
        return projectJpaRepository.findById(projectId)
                .map(ProjectEntity::getClient)
                .map(ClientEntityMapper::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Client> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Client> findAll(PageRequest request) {
        return SpringPageSupport.toDomain(
                jpaRepository.findAll(SpringPageSupport.toPageable(request)), this::toDomain);
    }

    @Override
    @Transactional
    public void delete(Client client) {
        jpaRepository.deleteById(client.getId());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(Client client) {
        jpaRepository.findById(client.getId())
                .ifPresent(entity -> ClientEntityMapper.apply(client, entity));
    }

    private Client toDomain(ClientEntity entity) {
        Client client = ClientEntityMapper.toDomain(entity);
        if (client == null) {
            return null;
        }
        projectJpaRepository.findByClientId(client.getId())
                .forEach(project -> client.addProject(ProjectEntityMapper.toDomainShallow(project)));
        return client;
    }
}
