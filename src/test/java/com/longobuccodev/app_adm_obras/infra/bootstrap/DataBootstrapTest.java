package com.longobuccodev.app_adm_obras.infra.bootstrap;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;
import com.longobuccodev.app_adm_obras.infra.security.BCryptPasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@SpringBootTest
@ActiveProfiles("test")
class DataBootstrapTest {

    static final Instant CLOCK_NOW = Instant.parse("2026-01-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(CLOCK_NOW, ZoneOffset.UTC);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${app.bootstrap.enabled}")
    private boolean bootstrapEnabled;

    @Test
    @DisplayName("o perfil test deve habilitar o bootstrap e usar H2 em memoria")
    void testProfileShouldEnableBootstrapOnH2() {
        assertThat(bootstrapEnabled).isTrue();
        assertThat(datasourceUrl).contains("h2:mem").doesNotContain("postgresql");
    }

    @Test
    @DisplayName("deve criar o usuario admin@admin.com com role ADMIN")
    void shouldCreateDefaultAdmin() {
        User admin = userRepository.findByEmail("admin@admin.com");

        assertThat(admin).isNotNull();
        assertThat(admin.getName()).isEqualTo("Administrador");
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.hasRole(Role.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("a senha do bootstrap deve estar hasheada e casar com admin123")
    void defaultAdminPasswordShouldMatchDocumentedCredential() {
        User admin = userRepository.findByEmail("admin@admin.com");

        assertThat(admin.getPasswordHash()).isNotEqualTo("admin123").startsWith("$2");
        assertThat(passwordHasher.matches("admin123", admin.getPasswordHash())).isTrue();
        assertThat(passwordHasher.matches("senha-errada", admin.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("o bootstrap nao deve duplicar o usuario quando executado novamente")
    void shouldBeIdempotent() {
        int before = userRepository.findAll().size();

        runBootstrap();

        assertThat(userRepository.findAll()).hasSize(before);
    }

    @Test
    @DisplayName("nao deve falhar quando o bootstrap nao esta configurado")
    void shouldSkipWhenNotConfigured() {
        new DataBootstrap(userRepository, passwordHasher,
                new BootstrapProperties(new BootstrapProperties.Admin(null, null, null)), CLOCK)
                .run(null);

        assertThat(userRepository.findByEmail("admin@admin.com")).isNotNull();
    }

    @Test
    @DisplayName("o perfil test nao deve criar usuario com senha em claro")
    void shouldNotPersistPlainTextPassword() {
        assertThat(userRepository.findAll())
                .noneMatch(user -> "admin123".equals(user.getPasswordHash()));
    }

    @Test
    @DisplayName("o hasher injetado deve ser BCrypt")
    void passwordHasherShouldBeBcrypt() {
        assertThat(passwordHasher).isInstanceOf(BCryptPasswordHasher.class);
    }

    private void runBootstrap() {
        new DataBootstrap(userRepository, passwordHasher,
                new BootstrapProperties(new BootstrapProperties.Admin("Administrador", "admin@admin.com", "admin123")), CLOCK)
                .run(null);
    }
}
