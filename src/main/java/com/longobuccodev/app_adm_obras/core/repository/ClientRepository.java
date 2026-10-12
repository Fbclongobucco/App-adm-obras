package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;

import java.util.List;
import java.util.UUID;

public interface ClientRepository {

    Client save(Client client);
    Client findById(UUID id);
    Client findByProjectId(UUID projectId);

    Client findByEmail(String email);
    List<Client> findAll();
    default Page<Client> findAll(PageRequest request) {
        return findAll(request, SearchTerm.NONE);
    }

    /** Pagina filtrada pelo termo nos campos pesquisaveis do recurso; {@link SearchTerm#NONE} nao filtra. */
    Page<Client> findAll(PageRequest request, SearchTerm term);
    void delete(Client client);
    void deleteById(UUID id);
    void update(Client client);
}
