package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.auth.LoginRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.exception.AuthenticationFailedException;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.AuthenticationGateway;

public class AuthUseCase {

    private final AuthenticationGateway authenticationGateway;
    private final UserRepository userRepository;

    public AuthUseCase(AuthenticationGateway authenticationGateway, UserRepository userRepository) {
        this.authenticationGateway = authenticationGateway;
        this.userRepository = userRepository;
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        if (dto == null || dto.email() == null || dto.email().isBlank()
                || dto.password() == null || dto.password().isBlank()) {
            throw AuthenticationFailedException.invalidCredentials();
        }
        User user = authenticationGateway.authenticate(dto.email(), dto.password());
        return toResponse(user);
    }

    public LoginResponseDTO profile(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw AuthenticationFailedException.unknownEmail(email);
        }
        return toResponse(user);
    }

    private LoginResponseDTO toResponse(User user) {
        return new LoginResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRoles());
    }
}
