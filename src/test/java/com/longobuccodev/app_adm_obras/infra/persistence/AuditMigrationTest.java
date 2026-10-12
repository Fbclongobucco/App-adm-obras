package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Migra um banco proprio ate o V2, grava dados como antes da auditoria e aplica o V3. */
class AuditMigrationTest {

    @Test
    @DisplayName("registros gravados antes do V3 devem receber datas preenchidas")
    void preExistingRowsShouldReceiveTimestamps() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:audit-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Flyway.configure().dataSource(dataSource).target("2").load().migrate();
        jdbc.update("insert into cost_center (id, name, cnpj) values (random_uuid(), 'Centro Antigo', '11222333000181')");

        Flyway.configure().dataSource(dataSource).load().migrate();

        Map<String, Object> row = jdbc.queryForMap("select created_at, updated_at from cost_center");
        assertThat(row.get("CREATED_AT")).isNotNull();
        assertThat(row.get("UPDATED_AT")).isNotNull();
    }
}
