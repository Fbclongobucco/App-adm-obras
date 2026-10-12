package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;

import java.util.List;
import java.util.UUID;

public interface UserRepository {

    User save(User user);
    User findById(UUID id);
    User findByEmail(String email);
    List<User> findAll();
    default Page<User> findAll(PageRequest request) {
        return findAll(request, SearchTerm.NONE);
    }

    /** Pagina filtrada pelo termo nos campos pesquisaveis do recurso; {@link SearchTerm#NONE} nao filtra. */
    Page<User> findAll(PageRequest request, SearchTerm term);
    void delete(User user);
    void deleteById(UUID id);
    void update(User user);
}
