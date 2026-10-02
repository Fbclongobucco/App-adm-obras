package com.longobuccodev.app_adm_obras.infra.security;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.exception.AuthenticationFailedException;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.AuthenticationGateway;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAuthenticationGateway implements AuthenticationGateway {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public DatabaseAuthenticationGateway(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw AuthenticationFailedException.unknownEmail(email);
        }
        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw AuthenticationFailedException.invalidCredentials();
        }
        if (!user.isActive()) {
            throw AuthenticationFailedException.invalidCredentials();
        }
        return user;
    }
}
