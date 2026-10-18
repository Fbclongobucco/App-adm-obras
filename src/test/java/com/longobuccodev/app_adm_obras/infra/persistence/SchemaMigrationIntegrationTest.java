package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O schema vem so das migracoes. Com ddl-auto=validate, o contexto so sobe se
 * cada entidade JPA bater, tabela a tabela e coluna a coluna, com o schema criado
 * pelo Flyway.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ActiveProfiles("test")
class SchemaMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("banco vazio deve receber todas as migracoes, e as entidades devem bater com o schema")
    void emptyDatabaseShouldBeMigratedAndMatchEntities() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("10");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'refresh_token'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("V9 deve criar as tabelas de auditoria de alteracoes e de login")
    void auditTablesShouldExist() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables "
                + "where table_name in ('change_audit', 'login_audit')", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("nova inicializacao no mesmo banco nao deve reaplicar nada nem perder dados")
    void repeatedMigrationShouldBeNoOp() {
        Integer usersBefore = jdbcTemplate.queryForObject("select count(*) from app_user", Integer.class);
        assertThat(usersBefore).as("usuario do bootstrap").isPositive();

        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from app_user", Integer.class)).isEqualTo(usersBefore);
    }

    @Test
    @DisplayName("refeicao sem restaurante nem obra deve ser rejeitada pelo banco (V3)")
    void mealWithoutRestaurantShouldBeRejected() {
        assertThatThrownBy(() -> jdbcTemplate.update("insert into meal (id, meal_type, lunch_quantity, "
                + "lunch_price, lunch_slot, is_billed, created_at, updated_at) values (random_uuid(), 'LUNCH', 1, "
                + "10, true, false, current_timestamp, current_timestamp)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
