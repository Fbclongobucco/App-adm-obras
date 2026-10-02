package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.infra.entities.UserEntity;

import java.util.EnumSet;

public final class UserEntityMapper {

    private UserEntityMapper() {
    }

    public static UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        UserEntity entity = UserEntity.create();
        apply(user, entity);
        return entity;
    }

    public static void apply(User user, UserEntity entity) {
        if (user == null || entity == null) {
            return;
        }
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setIsActive(user.getIsActive());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRoles(user.getRoles().isEmpty()
                ? EnumSet.noneOf(Role.class)
                : EnumSet.copyOf(user.getRoles()));
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        User user = new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getIsActive(),
                entity.getPasswordHash());
        if (entity.getRoles() != null) {
            entity.getRoles().forEach(user::addRole);
        }
        return user;
    }
}
