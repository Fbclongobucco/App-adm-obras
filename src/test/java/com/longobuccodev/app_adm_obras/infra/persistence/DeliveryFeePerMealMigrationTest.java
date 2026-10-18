package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Migra um banco proprio ate o V9 com restaurantes e um ajuste e aplica o V10 (entrega por refeicao). */
class DeliveryFeePerMealMigrationTest {

    private static final String NOW = "current_timestamp, current_timestamp";

    @Test
    @DisplayName("a taxa atual vale para almoco e jantar; ajustes existentes ficam com entregas e refrigerantes no padrao")
    void deliveryFeeCopiedToBothMeals() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:delivery-fee-per-meal-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("9").load().migrate();

        UUID address = UUID.randomUUID();
        UUID costCenter = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        UUID project = UUID.randomUUID();
        UUID withFee = UUID.randomUUID();
        UUID withoutFee = UUID.randomUUID();
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
        jdbc.update("insert into restaurant (id, name, project_id, delivery_fee, created_at, updated_at) "
                + "values (?, 'Cantina com taxa', ?, 8.00, " + NOW + ")", withFee, project);
        jdbc.update("insert into restaurant (id, name, project_id, created_at, updated_at) "
                + "values (?, 'Cantina sem taxa', ?, " + NOW + ")", withoutFee, project);
        jdbc.update("insert into meal (id, meal_type, lunch_quantity, lunch_price, lunch_slot, is_billed, "
                + "restaurant_id, project_id, created_at, updated_at) values (?, 'LUNCH', 10, 25.00, true, false, "
                + "?, ?, " + NOW + ")", meal, withFee, project);
        jdbc.update("insert into meal_adjustment (id, meal_id, date, type, lunch_quantity, created_at, updated_at) "
                + "values (random_uuid(), ?, date '2026-10-10', 'EXTRA_DAY', 6, " + NOW + ")", meal);

        Flyway.configure().dataSource(dataSource).load().migrate();

        Map<String, Object> migrated = jdbc.queryForMap("select * from restaurant where id = ?", withFee);
        assertThat(migrated).doesNotContainKey("DELIVERY_FEE");
        assertThat((BigDecimal) migrated.get("LUNCH_DELIVERY_FEE")).isEqualByComparingTo("8.00");
        assertThat((BigDecimal) migrated.get("DINNER_DELIVERY_FEE")).isEqualByComparingTo("8.00");
        Map<String, Object> noFee = jdbc.queryForMap("select * from restaurant where id = ?", withoutFee);
        assertThat(noFee.get("LUNCH_DELIVERY_FEE")).isNull();
        assertThat(noFee.get("DINNER_DELIVERY_FEE")).isNull();
        Map<String, Object> adjustment = jdbc.queryForMap("select * from meal_adjustment where meal_id = ?", meal);
        assertThat(adjustment.get("LUNCH_DELIVERIES")).isNull();
        assertThat(adjustment.get("DINNER_DELIVERIES")).isNull();
        assertThat(adjustment.get("SODAS")).isNull();
        jdbc.update("insert into meal_adjustment (id, meal_id, date, type, lunch_quantity, lunch_deliveries, sodas, "
                + "created_at, updated_at) values (random_uuid(), ?, date '2026-10-14', 'CHANGED', 9, 2, 3, " + NOW
                + ")", meal);
        assertThat(jdbc.queryForObject("select count(*) from meal_adjustment where type = 'CHANGED'", Integer.class))
                .isEqualTo(1);
    }
}
