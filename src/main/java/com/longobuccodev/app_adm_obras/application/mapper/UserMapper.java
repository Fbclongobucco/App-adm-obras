package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.UserRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.UserResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;

import java.util.UUID;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserRequestDTO dto, PasswordHasher hasher) {
        return toDomain(null, dto, hasher);
    }

    public static User toDomain(UUID id, UserRequestDTO dto, PasswordHasher hasher) {
        User user = new User(id, dto.name(), dto.email(), dto.isActive());
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPasswordHash(hasher.hash(dto.password()));
        }
        if (dto.roles() != null) {
            dto.roles().forEach(user::addRole);
        }
        return user;
    }

    public static UserResponseDTO toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getIsActive(),
                user.getRoles());
    }
}
