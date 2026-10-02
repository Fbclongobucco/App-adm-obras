package com.longobuccodev.app_adm_obras.application.dto.user;

import com.longobuccodev.app_adm_obras.core.domain.User.Role;

import java.util.Set;

public record UserRequestDTO(
        String name,
        String email,
        Boolean isActive,
        Set<Role> roles,
        String password
) {
}
