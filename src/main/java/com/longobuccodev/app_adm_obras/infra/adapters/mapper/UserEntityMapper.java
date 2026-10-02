package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.infra.entities.UserEntity;

import java.util.EnumSet;

public final class UserEntityMapper {

    private UserEntityMapper() {
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        User user = new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getIsActive());
        if (entity.getRoles() != null) {
            entity.getRoles().forEach(user::addRole);
        }
        return user;
    }

    public static UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }
        UserEntity entity = UserEntity.create();
        entity.setId(domain.getId());
        apply(domain, entity);
        return entity;
    }

    public static void apply(User domain, UserEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setIsActive(domain.getIsActive());
        entity.setRoles(domain.getRoles().isEmpty()
                ? EnumSet.noneOf(User.Role.class)
                : EnumSet.copyOf(domain.getRoles()));
    }
}
