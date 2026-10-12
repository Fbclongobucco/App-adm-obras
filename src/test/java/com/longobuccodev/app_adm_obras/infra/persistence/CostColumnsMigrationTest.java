package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Migra um banco proprio ate o V4 com restaurante e refeicao e aplica o V5 (taxa de entrega e refrigerante). */
class CostColumnsMigrationTest {

    private static final String NOW = "current_timestamp, current_timestamp";

    @Test
    @DisplayName("restaurante e refeicao existentes devem ficar sem taxa e sem refrigerante, em colunas anulaveis")
    void existingRowsShouldHaveNoDeliveryFeeNorSoda() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:cost-columns-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("4").load().migrate();

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
        jdbc.update("insert into project (id, os, start_date, is_completed, total_price, cost_center_id, client_id, "
                + "created_at, updated_at) values (?, 'OS-1', current_date, false, 0, ?, ?, " + NOW + ")",
                project, costCenter, client);
        jdbc.update("insert into restaurant (id, name, project_id, created_at, updated_at) "
                + "values (?, 'Cantina da Obra', ?, " + NOW + ")", restaurant, project);
        jdbc.update("insert into meal (id, meal_type, lunch_quantity, lunch_price, lunch_slot, is_billed, "
                + "restaurant_id, project_id, created_at, updated_at) values (?, 'LUNCH', 10, 25.00, true, false, "
                + "?, ?, " + NOW + ")", meal, restaurant, project);

        // Ate o V5: o V6 move soda_price para restaurant (SodaPriceMigrationTest)
        Flyway.configure().dataSource(dataSource).target("5").load().migrate();

        Map<String, Object> migratedRestaurant = jdbc.queryForMap("select * from restaurant where id = ?", restaurant);
        assertThat(migratedRestaurant).containsKey("DELIVERY_FEE");
        assertThat(migratedRestaurant.get("DELIVERY_FEE")).isNull();
        Map<String, Object> migratedMeal = jdbc.queryForMap("select * from meal where id = ?", meal);
        assertThat(migratedMeal).containsKey("SODA_PRICE");
        assertThat(migratedMeal.get("SODA_PRICE")).isNull();
        assertThat((BigDecimal) migratedMeal.get("LUNCH_PRICE")).isEqualByComparingTo("25.00");

        List<Map<String, Object>> columns = jdbc.queryForList("select table_name, column_name, is_nullable, "
                + "numeric_precision, numeric_scale from information_schema.columns "
                + "where lower(column_name) in ('delivery_fee', 'soda_price') order by table_name");
        assertThat(columns).extracting(row -> row.get("TABLE_NAME").toString().toLowerCase() + "."
                        + row.get("COLUMN_NAME").toString().toLowerCase() + " " + row.get("IS_NULLABLE") + " "
                        + row.get("NUMERIC_PRECISION") + "," + row.get("NUMERIC_SCALE"))
                .containsExactly("meal.soda_price YES 19,2", "restaurant.delivery_fee YES 19,2");

        jdbc.update("update restaurant set delivery_fee = 8.00 where id = ?", restaurant);
        jdbc.update("update meal set soda_price = 5.00 where id = ?", meal);
        assertThat(jdbc.queryForObject("select delivery_fee from restaurant where id = ?", BigDecimal.class,
                restaurant)).isEqualByComparingTo("8.00");
    }
}
