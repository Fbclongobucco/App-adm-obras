package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Migra um banco proprio ate o V6 com uma refeicao e aplica o V7 (ajustes por data da refeicao). */
class MealAdjustmentMigrationTest {

    private static final String NOW = "current_timestamp, current_timestamp";

    @Test
    @DisplayName("meal_adjustment: uma data por refeicao e exclusao em cascata com a refeicao")
    void mealAdjustmentTable() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:meal-adjustment-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("6").load().migrate();

        UUID address = UUID.randomUUID();
        UUID costCenter = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        UUID project = UUID.randomUUID();
        UUID restaurant = UUID.randomUUID();
        UUID meal = UUID.randomUUID();
        jdbc.update("insert into address (id, street, number, city, state, country, neighborhood, zip_code) "
                + "values (?, 'Rua B', '2', 'Campinas', 'SP', 'Brasil', 'Centro', '13010000')", address);
        jdbc.update("insert into cost_center (id, name, cnpj, created_at, updated_at) "
                + "values (?, 'Centro', '11222333000181', " + NOW + ")", costCenter);
        jdbc.update("insert into client (id, name, phone, address_id, created_at, updated_at) values (?, "
                + "'Cliente', '1933334444', ?, " + NOW + ")", client, address);
        jdbc.update("insert into project (id, os, start_date, end_date, is_completed, total_price, cost_center_id, "
                + "client_id, created_at, updated_at) values (?, 'OS-1', date '2026-10-05', date '2026-10-16', false, "
                + "0, ?, ?, " + NOW + ")", project, costCenter, client);
        jdbc.update("insert into restaurant (id, name, project_id, created_at, updated_at) "
                + "values (?, 'Cantina da Obra', ?, " + NOW + ")", restaurant, project);
        jdbc.update("insert into meal (id, meal_type, lunch_quantity, lunch_price, lunch_slot, is_billed, "
                + "restaurant_id, project_id, created_at, updated_at) values (?, 'LUNCH', 10, 25.00, true, false, "
                + "?, ?, " + NOW + ")", meal, restaurant, project);

        Flyway.configure().dataSource(dataSource).load().migrate();

        String insert = "insert into meal_adjustment (id, meal_id, date, type, lunch_quantity, dinner_quantity, note, "
                + "created_at, updated_at) values (random_uuid(), ?, ?, ?, ?, null, ?, " + NOW + ")";
        jdbc.update(insert, meal, java.sql.Date.valueOf("2026-10-10"), "EXTRA_DAY", 6, null);
        jdbc.update(insert, meal, java.sql.Date.valueOf("2026-10-12"), "NO_SERVICE", null, "Feriado");
        assertThat(jdbc.queryForObject("select count(*) from meal_adjustment where meal_id = ?", Integer.class, meal))
                .isEqualTo(2);
        assertThatThrownBy(() -> jdbc.update(insert, meal, java.sql.Date.valueOf("2026-10-12"), "NO_SERVICE",
                null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("UK_MEAL_ADJUSTMENT_MEAL_DATE");
        assertThatThrownBy(() -> jdbc.update(insert, meal, java.sql.Date.valueOf("2026-10-13"), "NO_SERVICE",
                null, "a".repeat(101)))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("delete from meal where id = ?", meal);

        assertThat(jdbc.queryForObject("select count(*) from meal_adjustment", Integer.class)).isZero();
    }
}
