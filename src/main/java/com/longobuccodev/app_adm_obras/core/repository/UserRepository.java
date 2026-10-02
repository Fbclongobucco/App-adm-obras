package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.User;

import java.util.List;
import java.util.UUID;

public interface UserRepository {

    User save(User user);
    User findById(UUID id);
    User findByEmail(String email);
    List<User> findAll();
    Page<User> findAll(PageRequest request);
    void delete(User user);
    void deleteById(UUID id);
    void update(User user);
}
