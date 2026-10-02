package com.longobuccodev.app_adm_obras.application.dto;

import com.longobuccodev.app_adm_obras.core.domain.User.Role;

import java.util.Set;
import java.util.UUID;

public record UserRequestDTO(
        String name,
        String email,
        Boolean isActive,
        Set<Role> roles
) {
}
