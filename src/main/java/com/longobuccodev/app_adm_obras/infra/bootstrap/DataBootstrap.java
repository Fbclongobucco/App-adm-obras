package com.longobuccodev.app_adm_obras.infra.bootstrap;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.bootstrap", name = "enabled", havingValue = "true")
public class DataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final BootstrapProperties properties;

    public DataBootstrap(UserRepository userRepository, PasswordHasher passwordHasher,
                         BootstrapProperties properties) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        BootstrapProperties.Admin admin = properties.admin();
        if (admin == null || admin.email() == null || admin.email().isBlank()
                || admin.password() == null || admin.password().isBlank()) {
            log.warn("Bootstrap habilitado mas app.bootstrap.admin.email/password nao definidos; nada foi criado");
            return;
        }
        if (userRepository.findByEmail(admin.email()) != null) {
            log.info("Usuario bootstrap {} ja existe; nada a fazer", admin.email());
            return;
        }

        String name = admin.name() == null || admin.name().isBlank() ? "Administrador" : admin.name();
        User user = new User(null, name, admin.email(), true, passwordHasher.hash(admin.password()));
        user.addRole(Role.ADMIN);
        userRepository.save(user);

        log.info("Usuario bootstrap criado: {} com role ADMIN", admin.email());
    }
}
