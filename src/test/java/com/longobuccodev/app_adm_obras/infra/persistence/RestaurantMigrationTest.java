package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Migra um banco proprio ate o V2 com refeicoes no formato antigo e aplica o V3. */
class RestaurantMigrationTest {

    @Test
    @DisplayName("refeicoes com o mesmo restaurante na mesma obra devem virar um unico restaurante com endereco")
    void mealsShouldBeMigratedToRestaurants() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:restaurant-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("2").load().migrate();

        UUID address = UUID.randomUUID();
        UUID costCenter = UUID.randomUUID();
        UUID clientAddress = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        UUID project = UUID.randomUUID();
        jdbc.update("insert into address (id, street, number, city, state, country, neighborhood, zip_code) "
                + "values (?, 'Rua A', '1', 'Campinas', 'SP', 'Brasil', 'Centro', '13010000')", address);
        jdbc.update("insert into address (id, street, number, city, state, country, neighborhood, zip_code) "
                + "values (?, 'Rua B', '2', 'Campinas', 'SP', 'Brasil', 'Centro', '13010000')", clientAddress);
        jdbc.update("insert into cost_center (id, name, cnpj) values (?, 'Centro', '11222333000181')", costCenter);
        jdbc.update("insert into client (id, name, email, phone, address_id) values (?, 'Cliente', "
                + "'c@exemplo.test', '1933334444', ?)", client, clientAddress);
        jdbc.update("insert into project (id, os, start_date, is_completed, total_price, cost_center_id, client_id) "
                + "values (?, 'OS-1', current_date, false, 0, ?, ?)", project, costCenter, client);
        for (String type : List.of("LUNCH", "DINNER")) {
            jdbc.update("insert into meal (id, restaurant_name, meal_type, date, price, quantity, total_price, "
                    + "is_billed, address_id, project_id) values (random_uuid(), 'Cantina da Obra', ?, current_date, "
                    + "10, 1, 10, false, ?, ?)", type, address, project);
        }

        Flyway.configure().dataSource(dataSource).load().migrate();

        List<Map<String, Object>> restaurants = jdbc.queryForList("select id, name, address_id, project_id "
                + "from restaurant");
        assertThat(restaurants).hasSize(1);
        Map<String, Object> restaurant = restaurants.get(0);
        assertThat(restaurant.get("NAME")).isEqualTo("Cantina da Obra");
        assertThat(restaurant.get("ADDRESS_ID")).isEqualTo(address);
        assertThat(restaurant.get("PROJECT_ID")).isEqualTo(project);
        assertThat(jdbc.queryForObject("select count(*) from meal where restaurant_id = ?", Integer.class,
                restaurant.get("ID"))).isEqualTo(2);
    }
}
