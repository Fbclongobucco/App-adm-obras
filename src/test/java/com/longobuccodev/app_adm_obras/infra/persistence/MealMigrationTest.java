package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Migra um banco proprio ate o V3 com refeicoes no formato antigo (data, quantidade, preco) e aplica o V4. */
class MealMigrationTest {

    private static final String NOW = "current_timestamp, current_timestamp";

    @Test
    @DisplayName("LUNCH e DINNER devem virar campos de almoco e jantar; email de cliente e colaborador anulavel")
    void mealsShouldBeMigratedToServedMealFields() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:meal-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("3").load().migrate();

        UUID costCenter = UUID.randomUUID();
        UUID clientAddress = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        UUID project = UUID.randomUUID();
        UUID restaurant = UUID.randomUUID();
        UUID lunch = UUID.randomUUID();
        UUID dinner = UUID.randomUUID();
        jdbc.update("insert into address (id, street, number, city, state, country, neighborhood, zip_code) "
                + "values (?, 'Rua B', '2', 'Campinas', 'SP', 'Brasil', 'Centro', '13010000')", clientAddress);
        jdbc.update("insert into cost_center (id, name, cnpj, created_at, updated_at) "
                + "values (?, 'Centro', '11222333000181', " + NOW + ")", costCenter);
        jdbc.update("insert into client (id, name, email, phone, address_id, created_at, updated_at) values (?, "
                + "'Cliente', 'c@exemplo.test', '1933334444', ?, " + NOW + ")", client, clientAddress);
        jdbc.update("insert into project (id, os, start_date, is_completed, total_price, cost_center_id, client_id, "
                + "created_at, updated_at) values (?, 'OS-1', current_date, false, 0, ?, ?, " + NOW + ")",
                project, costCenter, client);
        jdbc.update("insert into restaurant (id, name, project_id, created_at, updated_at) "
                + "values (?, 'Cantina da Obra', ?, " + NOW + ")", restaurant, project);
        insertOldMeal(jdbc, lunch, "LUNCH", 12, "30.00", true, restaurant, project);
        insertOldMeal(jdbc, dinner, "DINNER", 7, "28.50", false, restaurant, project);

        Flyway.configure().dataSource(dataSource).load().migrate();

        Map<String, Object> migratedLunch = jdbc.queryForMap("select * from meal where id = ?", lunch);
        assertThat(migratedLunch.get("MEAL_TYPE")).isEqualTo("LUNCH");
        assertThat(migratedLunch.get("LUNCH_QUANTITY")).isEqualTo(12);
        assertThat((BigDecimal) migratedLunch.get("LUNCH_PRICE")).isEqualByComparingTo("30.00");
        assertThat(migratedLunch.get("LUNCH_SLOT")).isEqualTo(true);
        assertThat(migratedLunch.get("DINNER_QUANTITY")).isNull();
        assertThat(migratedLunch.get("DINNER_PRICE")).isNull();
        assertThat(migratedLunch.get("DINNER_SLOT")).isNull();
        assertThat(migratedLunch.get("IS_BILLED")).isEqualTo(true);
        assertThat(migratedLunch.get("RESTAURANT_ID")).isEqualTo(restaurant);
        assertThat(migratedLunch).doesNotContainKeys("DATE", "QUANTITY", "PRICE", "TOTAL_PRICE");

        Map<String, Object> migratedDinner = jdbc.queryForMap("select * from meal where id = ?", dinner);
        assertThat(migratedDinner.get("DINNER_QUANTITY")).isEqualTo(7);
        assertThat((BigDecimal) migratedDinner.get("DINNER_PRICE")).isEqualByComparingTo("28.50");
        assertThat(migratedDinner.get("DINNER_SLOT")).isEqualTo(true);
        assertThat(migratedDinner.get("LUNCH_QUANTITY")).isNull();
        assertThat(migratedDinner.get("LUNCH_SLOT")).isNull();

        // Cobertura de almoco ja ocupada na obra: o banco rejeita outra
        assertThatThrownBy(() -> jdbc.update("insert into meal (id, meal_type, lunch_quantity, lunch_price, "
                + "dinner_quantity, dinner_price, lunch_slot, dinner_slot, is_billed, restaurant_id, project_id, "
                + "created_at, updated_at) values (random_uuid(), 'LUNCH_AND_DINNER', 1, 1, 1, 1, true, true, "
                + "false, ?, ?, " + NOW + ")", restaurant, project))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Varios clientes sem e-mail nao conflitam
        for (int i = 0; i < 2; i++) {
            jdbc.update("insert into client (id, name, email, phone, address_id, created_at, updated_at) "
                    + "values (random_uuid(), 'Sem email', null, '1933334444', ?, " + NOW + ")", clientAddress);
        }
        assertThat(jdbc.queryForObject("select count(*) from client where email is null", Integer.class))
                .isEqualTo(2);
        List<Map<String, Object>> nullableEmails = jdbc.queryForList("select table_name, is_nullable "
                + "from information_schema.columns where lower(column_name) = 'email' "
                + "and lower(table_name) in ('client', 'employee', 'app_user') order by table_name");
        assertThat(nullableEmails).extracting(row -> row.get("TABLE_NAME").toString().toLowerCase() + "="
                + row.get("IS_NULLABLE")).containsExactly("app_user=NO", "client=YES", "employee=YES");
    }

    private static void insertOldMeal(JdbcTemplate jdbc, UUID id, String type, int quantity, String price,
                                      boolean billed, UUID restaurant, UUID project) {
        BigDecimal unit = new BigDecimal(price);
        jdbc.update("insert into meal (id, meal_type, date, price, quantity, total_price, is_billed, restaurant_id, "
                        + "project_id, created_at, updated_at) values (?, ?, current_date, ?, ?, ?, ?, ?, ?, " + NOW + ")",
                id, type, unit, quantity, unit.multiply(BigDecimal.valueOf(quantity)), billed, restaurant, project);
    }
}
