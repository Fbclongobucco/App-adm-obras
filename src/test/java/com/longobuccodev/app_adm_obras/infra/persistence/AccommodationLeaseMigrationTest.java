package com.longobuccodev.app_adm_obras.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Migra um banco proprio ate o V7 com hospedagens e aplica o V8 (locacao mensal). */
class AccommodationLeaseMigrationTest {

    private JdbcTemplate jdbc;

    @Test
    @DisplayName("entrada pela obra ou pelo cadastro, valor mensal pelo total e primeiro periodo de um mes")
    void accommodationsShouldGetFirstPeriod() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:accommodation-lease-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("7").load().migrate();
        UUID project = project(LocalDate.of(2026, 10, 12));

        UUID withProject = accommodation(project, "3000.00", "2026-09-01 10:00:00");
        UUID withoutProject = accommodation(null, "1500.00", "2027-01-31 10:00:00");
        UUID zeroPrice = accommodation(null, "0.00", "2026-12-01 10:00:00");

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertLease(withProject, "2026-10-12", "2026-11-11", "3000.00");
        assertLease(withoutProject, "2027-01-31", "2027-02-28", "1500.00");
        assertLease(zeroPrice, "2026-12-01", "2026-12-31", "0.01");
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns "
                + "where lower(table_name) = 'accommodation' and lower(column_name) in ('days', 'total_price')",
                Integer.class)).isZero();
    }

    private void assertLease(UUID id, String start, String end, String price) {
        Map<String, Object> accommodation = jdbc.queryForMap(
                "select start_date, monthly_price from accommodation where id = ?", id);
        assertThat(accommodation.get("start_date").toString()).isEqualTo(start);
        assertThat((BigDecimal) accommodation.get("monthly_price")).isEqualByComparingTo(price);
        Map<String, Object> period = jdbc.queryForMap(
                "select id, start_date, end_date, price from accommodation_period where accommodation_id = ?", id);
        assertThat(period.get("id")).isEqualTo(id);
        assertThat(period.get("start_date").toString()).isEqualTo(start);
        assertThat(period.get("end_date").toString()).isEqualTo(end);
        assertThat((BigDecimal) period.get("price")).isEqualByComparingTo(price);
    }

    private UUID project(LocalDate start) {
        UUID address = address();
        UUID costCenter = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("insert into cost_center (id, name, cnpj, created_at, updated_at) "
                + "values (?, 'Centro', '11222333000181', current_timestamp, current_timestamp)", costCenter);
        jdbc.update("insert into client (id, name, phone, address_id, created_at, updated_at) values (?, "
                + "'Cliente', '1933334444', ?, current_timestamp, current_timestamp)", client, address);
        jdbc.update("insert into project (id, os, start_date, is_completed, total_price, cost_center_id, client_id, "
                + "created_at, updated_at) values (?, 'OS-1', ?, false, 0, ?, ?, current_timestamp, current_timestamp)",
                id, start, costCenter, client);
        return id;
    }

    private UUID address() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into address (id, street, number, city, state, country, neighborhood, zip_code) "
                + "values (?, 'Rua B', '2', 'Campinas', 'SP', 'Brasil', 'Centro', '13010000')", id);
        return id;
    }

    private UUID accommodation(UUID project, String totalPrice, String createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into accommodation (id, host_name, host_phone, address_id, capacity, days, is_contract, "
                + "total_price, project_id, created_at, updated_at) values (?, 'Pousada', '1933335555', ?, 4, 10, "
                + "true, ?, ?, cast(? as timestamp with time zone), current_timestamp)",
                id, address(), new BigDecimal(totalPrice), project, createdAt);
        return id;
    }
}
