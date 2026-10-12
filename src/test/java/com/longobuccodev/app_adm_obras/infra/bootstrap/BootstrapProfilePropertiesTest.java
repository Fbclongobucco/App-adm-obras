package com.longobuccodev.app_adm_obras.infra.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapProfilePropertiesTest {

    @Test
    void localProfileShouldCreateAdminByDefault() throws IOException {
        assertThat(load("application-local.properties").getProperty("app.bootstrap.enabled"))
                .isEqualTo("${APP_BOOTSTRAP_ENABLED:true}");
    }

    @Test
    void prodProfileShouldNeverCreateAdmin() throws IOException {
        assertThat(load("application-prod.properties").getProperty("app.bootstrap.enabled")).isEqualTo("false");
    }

    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = BootstrapProfilePropertiesTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            properties.load(in);
        }
        return properties;
    }
}
