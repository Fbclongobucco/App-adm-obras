package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Migra um banco proprio ate o V5 com refeicoes com refrigerante e aplica o V6 (refrigerante no restaurante). */
class SodaPriceMigrationTest {

    private static final String NOW = "current_timestamp, current_timestamp";

    private JdbcTemplate jdbc;
    private UUID project;

    @Test
    @DisplayName("cada restaurante recebe o maior refrigerante das refeicoes dele e meal.soda_price deixa de existir")
    void sodaPriceShouldMoveToRestaurant() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:soda-price-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("5").load().migrate();
        seedProject();

        // Precos diferentes: 5.00 no almoco e 6.00 no jantar
        UUID differentPrices = restaurant("Cantina Precos");
        meal(differentPrices, "LUNCH", "5.00");
        meal(differentPrices, "DINNER", "6.00");
        // Misto: almoco com 5.00 e jantar sem refrigerante (outra obra, para nao violar a cobertura unica)
        UUID mixed = restaurant("Cantina Mista", newProject("OS-2"));
        meal(mixed, "LUNCH", "5.00");
        meal(mixed, "DINNER", null);
        // Sem refrigerante em nenhuma refeicao
        UUID withoutSoda = restaurant("Cantina Seca", newProject("OS-3"));
        meal(withoutSoda, "LUNCH_AND_DINNER", null);
        // Sem refeicao
        UUID withoutMeals = restaurant("Cantina Vazia", newProject("OS-4"));

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(sodaPrice(differentPrices)).isEqualByComparingTo("6.00");
        assertThat(sodaPrice(mixed)).as("restaurante misto passa a fornecer a todas").isEqualByComparingTo("5.00");
        assertThat(sodaPrice(withoutSoda)).isNull();
        assertThat(sodaPrice(withoutMeals)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from meal", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns "
                + "where lower(table_name) = 'meal' and lower(column_name) = 'soda_price'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select is_nullable || ' ' || numeric_precision || ',' || numeric_scale "
                + "from information_schema.columns where lower(table_name) = 'restaurant' "
                + "and lower(column_name) = 'soda_price'", String.class)).isEqualTo("YES 19,2");
    }

    private void seedProject() {
        project = newProject("OS-1");
    }

    private UUID newProject(String os) {
        UUID address = UUID.randomUUID();
        UUID costCenter = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("insert into address (id, street, number, city, state, country, neighborhood, zip_code) "
                + "values (?, 'Rua B', '2', 'Campinas', 'SP', 'Brasil', 'Centro', '13010000')", address);
        jdbc.update("insert into cost_center (id, name, cnpj, created_at, updated_at) "
                + "values (?, 'Centro', '11222333000181', " + NOW + ")", costCenter);
        jdbc.update("insert into client (id, name, phone, address_id, created_at, updated_at) values (?, "
                + "'Cliente', '1933334444', ?, " + NOW + ")", client, address);
        jdbc.update("insert into project (id, os, start_date, is_completed, total_price, cost_center_id, client_id, "
                + "created_at, updated_at) values (?, ?, current_date, false, 0, ?, ?, " + NOW + ")",
                id, os, costCenter, client);
        return id;
    }

    private UUID restaurant(String name) {
        return restaurant(name, project);
    }

    private UUID restaurant(String name, UUID projectId) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into restaurant (id, name, project_id, created_at, updated_at) values (?, ?, ?, "
                + NOW + ")", id, name, projectId);
        return id;
    }

    private void meal(UUID restaurant, String type, String sodaPrice) {
        boolean lunch = !type.equals("DINNER");
        boolean dinner = !type.equals("LUNCH");
        UUID projectId = jdbc.queryForObject("select project_id from restaurant where id = ?", UUID.class, restaurant);
        jdbc.update("insert into meal (id, meal_type, lunch_quantity, lunch_price, lunch_slot, dinner_quantity, "
                        + "dinner_price, dinner_slot, soda_price, is_billed, restaurant_id, project_id, created_at, "
                        + "updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?, ?, " + NOW + ")",
                UUID.randomUUID(), type,
                lunch ? 10 : null, lunch ? new BigDecimal("25.00") : null, lunch ? Boolean.TRUE : null,
                dinner ? 8 : null, dinner ? new BigDecimal("22.00") : null, dinner ? Boolean.TRUE : null,
                sodaPrice == null ? null : new BigDecimal(sodaPrice), restaurant, projectId);
    }

    private BigDecimal sodaPrice(UUID restaurant) {
        return jdbc.queryForObject("select soda_price from restaurant where id = ?", BigDecimal.class, restaurant);
    }
}
