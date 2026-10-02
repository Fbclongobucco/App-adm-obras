package com.longobuccodev.app_adm_obras.application.dto.auth;

import com.longobuccodev.app_adm_obras.core.domain.User.Role;

import java.util.Set;
import java.util.UUID;

public record LoginResponseDTO(
        UUID id,
        String name,
        String email,
        Set<Role> roles
) {
}
