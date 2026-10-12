package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.common.ErrorResponseDTO;
import com.longobuccodev.app_adm_obras.support.AdminApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Forca cada violacao direto no banco, contornando os casos de uso, e confere que a
 * excecao real do banco e traduzida para o codigo esperado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class IntegrityViolationTranslatorTest {

    private static final String NOW = "current_timestamp, current_timestamp";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("violacoes de unicidade devem virar duplicate com o codigo do recurso")
    void uniqueViolations() {
        AdminApi api = new AdminApi(rest);
        String projectId = api.createProject();
        String os = jdbc.queryForObject("select os from project where id = ?", String.class, UUID.fromString(projectId));
        UUID costCenter = jdbc.queryForObject("select cost_center_id from project where id = ?", UUID.class,
                UUID.fromString(projectId));
        UUID client = jdbc.queryForObject("select client_id from project where id = ?", UUID.class,
                UUID.fromString(projectId));

        assertCode("insert into project (id, os, start_date, is_completed, total_price, cost_center_id, client_id, "
                + "created_at, updated_at) values (random_uuid(), '" + os + "', current_date, false, 0, '" + costCenter
                + "', '" + client + "', " + NOW + ")", "project.duplicate.os");

        String email = jdbc.queryForObject("select email from client where id = ?", String.class, client);
        UUID address = jdbc.queryForObject("select address_id from client where id = ?", UUID.class, client);
        assertCode("insert into client (id, name, email, phone, address_id, created_at, updated_at) values ("
                + "random_uuid(), 'Cliente Copia', '" + email + "', '1933334444', '" + address + "', " + NOW + ")",
                "client.duplicate.email");

        assertCode("insert into app_user (id, name, email, is_active, password_hash, created_at, updated_at) "
                + "values (random_uuid(), 'Copia', 'admin@admin.com', true, 'x', " + NOW + ")", "user.duplicate.email");
    }

    @Test
    @DisplayName("CPF, e-mail e vinculo de colaborador devem virar os codigos de colaborador")
    void employeeViolations() {
        AdminApi api = new AdminApi(rest);
        String costCenterId = api.createCostCenter();
        String employeeId = api.create("/api/employees", api.employeeBody(costCenterId));
        UUID id = UUID.fromString(employeeId);
        String cpf = jdbc.queryForObject("select cpf from employee where id = ?", String.class, id);
        String email = jdbc.queryForObject("select email from employee where id = ?", String.class, id);
        String insert = "insert into employee (id, name, email, cpf, birth_date, role, cost_center_id, created_at, "
                + "updated_at) values (random_uuid(), 'Copia', '%s', '%s', date '1990-01-01', 'MONTADOR', '"
                + costCenterId + "', " + NOW + ")";

        assertCode(insert.formatted(email, "52998224725"), "employee.duplicate.email");
        assertCode(insert.formatted(AdminApi.unique("x") + "@exemplo.test", cpf), "employee.duplicate.cpf");

        String projectId = api.createProject();
        jdbc.update("insert into project_employee (project_id, employee_id) values (?, ?)",
                UUID.fromString(projectId), id);
        assertCode("delete from employee where id = '" + id + "'", "employee.in_use");
    }

    @Test
    @DisplayName("duas coberturas de almoco ou de jantar na mesma obra e exclusao de restaurante com refeicao")
    void mealAndRestaurantViolations() {
        AdminApi api = new AdminApi(rest);
        String projectId = api.createProject();
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(projectId));
        String mealId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH_AND_DINNER"));
        String adjustment = "insert into meal_adjustment (id, meal_id, date, type, created_at, updated_at) values ("
                + "random_uuid(), '" + mealId + "', date '2026-01-12', 'NO_SERVICE', " + NOW + ")";
        jdbc.update(adjustment);
        assertCode(adjustment, "meal.adjustment.duplicate.date");

        assertCode("insert into meal (id, meal_type, lunch_quantity, lunch_price, lunch_slot, is_billed, "
                + "restaurant_id, project_id, created_at, updated_at) values (random_uuid(), 'LUNCH', 1, 10, true, "
                + "false, '" + restaurantId + "', '" + projectId + "', " + NOW + ")", "meal.duplicate.type");
        assertCode("insert into meal (id, meal_type, dinner_quantity, dinner_price, dinner_slot, is_billed, "
                + "restaurant_id, project_id, created_at, updated_at) values (random_uuid(), 'DINNER', 1, 10, true, "
                + "false, '" + restaurantId + "', '" + projectId + "', " + NOW + ")", "meal.duplicate.type");
        assertCode("delete from restaurant where id = '" + restaurantId + "'", "restaurant.in_use");
        assertCode("delete from project where id = '" + projectId + "'", "project.in_use");
    }

    @Test
    @DisplayName("violacoes de chave estrangeira na exclusao devem virar in_use do recurso referenciado")
    void foreignKeyViolations() {
        AdminApi api = new AdminApi(rest);
        String projectId = api.createProject();
        UUID costCenter = jdbc.queryForObject("select cost_center_id from project where id = ?", UUID.class,
                UUID.fromString(projectId));
        UUID client = jdbc.queryForObject("select client_id from project where id = ?", UUID.class,
                UUID.fromString(projectId));

        assertCode("delete from cost_center where id = '" + costCenter + "'", "cost_center.in_use");
        assertCode("delete from client where id = '" + client + "'", "client.in_use");
    }

    @Test
    @DisplayName("restricao desconhecida deve virar data.integrity_violation, e a mensagem nao deve expor o banco")
    void unknownConstraintAndSafeMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement [Referential integrity constraint violation: \"FK_OUTRA: "
                        + "PUBLIC.TABELA FOREIGN KEY(COLUNA)\"]");
        ResponseEntity<ErrorResponseDTO> response = new GlobalExceptionHandler()
                .handleIntegrityViolation(ex, new MockHttpServletRequest("DELETE", "/api/x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().errorCode()).isEqualTo("data.integrity_violation");
        assertThat(response.getBody().message()).doesNotContainIgnoringCase("tabela")
                .doesNotContainIgnoringCase("coluna").doesNotContainIgnoringCase("fk_");
    }

    private void assertCode(String sql, String expectedCode) {
        DataIntegrityViolationException ex = catchThrowableOfType(DataIntegrityViolationException.class,
                () -> jdbc.execute(sql));
        assertThat(ex).as(sql).isNotNull();
        assertThat(IntegrityViolationTranslator.errorCode(ex)).as(ex.getMessage()).isEqualTo(expectedCode);
    }
}
