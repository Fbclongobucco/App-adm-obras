package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Client;

import java.util.List;
import java.util.UUID;

public interface ClientRepository {

    Client save(Client client);
    Client findById(UUID id);
    Client findByProjectId(UUID projectId);
    List<Client> findAll();
    void delete(Client client);
    void deleteById(UUID id);
    void update(Client client);
}
