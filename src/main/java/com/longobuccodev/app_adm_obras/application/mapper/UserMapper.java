package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.UserRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.UserResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.User;

import java.util.UUID;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserRequestDTO dto) {
        return toDomain(null, dto);
    }

    public static User toDomain(UUID id, UserRequestDTO dto) {
        User user = new User(id, dto.name(), dto.email(), dto.isActive());
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
