package com.longobuccodev.app_adm_obras.infra.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O perfil prod exige PostgreSQL e nao sobe nos testes; a garantia de que a
 * documentacao nao e servida em producao fica fixada nas propriedades.
 */
class OpenApiProfilePropertiesTest {

    @ParameterizedTest(name = "{0} deve manter a documentacao desligada")
    @ValueSource(strings = {"application.properties", "application-prod.properties"})
    void documentationShouldBeDisabled(String resource) throws IOException {
        Properties properties = load(resource);

        assertThat(properties.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }

    @ParameterizedTest(name = "{0} deve ligar a documentacao")
    @ValueSource(strings = {"application-local.properties", "application-test.properties"})
    void documentationShouldBeEnabled(String resource) throws IOException {
        Properties properties = load(resource);

        assertThat(properties.getProperty("springdoc.api-docs.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("true");
    }

    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = OpenApiProfilePropertiesTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            properties.load(in);
        }
        return properties;
    }
}
