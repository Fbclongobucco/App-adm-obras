package com.longobuccodev.app_adm_obras.infra.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O perfil prod exige PostgreSQL e nao sobe nos testes; as garantias de
 * segredo e cookie em producao ficam fixadas nas propriedades.
 */
class SecurityProfilePropertiesTest {

    @Test
    @DisplayName("prod deve exigir APP_JWT_SECRET sem valor padrao")
    void prodShouldRequireSecretVariable() throws IOException {
        assertThat(load("application-prod.properties").getProperty("app.security.jwt.secret"))
                .isEqualTo("${APP_JWT_SECRET}");
    }

    @Test
    @DisplayName("prod deve fixar o cookie como Secure, sem placeholder")
    void prodShouldForceSecureCookie() throws IOException {
        assertThat(load("application-prod.properties").getProperty("app.security.refresh-cookie.secure"))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("application.properties nao deve definir segredo e deve ter SameSite Strict por padrao")
    void baseShouldHaveNoSecretAndStrictDefault() throws IOException {
        Properties properties = load("application.properties");

        assertThat(properties.getProperty("app.security.jwt.secret")).isNull();
        assertThat(properties.getProperty("app.security.refresh-cookie.same-site"))
                .isEqualTo("${APP_REFRESH_COOKIE_SAME_SITE:Strict}");
        assertThat(properties.getProperty("app.security.jwt.access-token-ttl")).isEqualTo("15m");
        assertThat(properties.getProperty("app.security.jwt.refresh-token-ttl")).isEqualTo("7d");
    }

    @ParameterizedTest(name = "{0} deve ter segredo de desenvolvimento e cookie sem Secure")
    @ValueSource(strings = {"application-local.properties", "application-test.properties"})
    void devProfilesShouldHaveDevSecret(String resource) throws IOException {
        Properties properties = load(resource);

        assertThat(properties.getProperty("app.security.jwt.secret")).isNotBlank().doesNotContain("${");
        assertThat(properties.getProperty("app.security.refresh-cookie.secure")).isEqualTo("false");
    }

    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = SecurityProfilePropertiesTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            properties.load(in);
        }
        return properties;
    }
}
