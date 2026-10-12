package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.support.AdminApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class ConflictResponsesIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    // --- duplicidade ---

    @Test
    @DisplayName("OS repetida na criacao deve responder 409 project.duplicate.os sem gravar")
    void duplicateOsOnCreate() {
        Map<String, Object> first = api.projectBody(api.createCostCenter(), api.createClient());
        api.create("/api/projects", first);
        Map<String, Object> second = api.projectBody(api.createCostCenter(), api.createClient());
        second.put("os", ((String) first.get("os")).toLowerCase());
        long before = total("/api/projects");

        ResponseEntity<Map> response = api.post("/api/projects", second);

        assertConflict(response, "project.duplicate.os");
        assertThat(total("/api/projects")).isEqualTo(before);
    }

    @Test
    @DisplayName("alterar a obra mantendo a propria OS deve ser aceito")
    void keepingOwnOsIsNotAConflict() {
        Map<String, Object> body = api.projectBody(api.createCostCenter(), api.createClient());
        String id = api.create("/api/projects", body);

        body.put("description", "Descricao nova");
        assertThat(api.put("/api/projects/" + id, body).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("CPF de outro colaborador na alteracao deve responder 409 e manter os dados")
    void duplicateCpfOnUpdate() {
        String costCenterId = api.createCostCenter();
        Map<String, Object> first = api.employeeBody(costCenterId);
        api.create("/api/employees", first);
        Map<String, Object> second = api.employeeBody(costCenterId);
        String secondId = api.create("/api/employees", second);
        String originalCpf = (String) second.get("cpf");

        second.put("cpf", first.get("cpf"));
        ResponseEntity<Map> response = api.put("/api/employees/" + secondId, second);

        assertConflict(response, "employee.duplicate.cpf");
        assertThat(api.get("/api/employees/" + secondId).getBody()).containsEntry("cpf", originalCpf);
    }

    @Test
    @DisplayName("e-mail de outro colaborador deve responder 409 employee.duplicate.email")
    void duplicateEmployeeEmail() {
        String costCenterId = api.createCostCenter();
        Map<String, Object> first = api.employeeBody(costCenterId);
        api.create("/api/employees", first);
        Map<String, Object> second = api.employeeBody(costCenterId);
        second.put("email", first.get("email"));

        assertConflict(api.post("/api/employees", second), "employee.duplicate.email");
    }

    @Test
    @DisplayName("e-mail de cliente com caixa diferente deve responder 409 client.duplicate.email")
    void duplicateClientEmailIgnoringCase() {
        Map<String, Object> first = api.clientBody();
        api.create("/api/clients", first);
        Map<String, Object> second = api.clientBody();
        second.put("email", ((String) first.get("email")).toUpperCase());

        assertConflict(api.post("/api/clients", second), "client.duplicate.email");
    }

    @Test
    @DisplayName("e-mail de outro usuario na criacao e na alteracao deve responder 409 user.duplicate.email")
    void duplicateUserEmail() {
        Map<String, Object> first = userBody();
        api.create("/api/users", first);
        Map<String, Object> second = userBody();
        String secondId = api.create("/api/users", second);

        Map<String, Object> repeated = userBody();
        repeated.put("email", first.get("email"));
        assertConflict(api.post("/api/users", repeated), "user.duplicate.email");

        second.put("email", first.get("email"));
        assertConflict(api.put("/api/users/" + secondId, second), "user.duplicate.email");
    }

    // --- exclusao com dependentes ---

    @Test
    @DisplayName("centro de custo com colaboradores deve responder 409 cost_center.in_use e continuar existindo")
    void costCenterWithEmployees() {
        String costCenterId = api.createCostCenter();
        api.create("/api/employees", api.employeeBody(costCenterId));

        ResponseEntity<Map> response = api.delete("/api/cost-centers/" + costCenterId);

        assertConflict(response, "cost_center.in_use");
        assertThat((String) response.getBody().get("message")).contains("employees");
        assertThat(api.get("/api/cost-centers/" + costCenterId).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("centro de custo com obras deve responder 409 cost_center.in_use")
    void costCenterWithProjects() {
        String costCenterId = api.createCostCenter();
        api.create("/api/projects", api.projectBody(costCenterId, api.createClient()));

        assertConflict(api.delete("/api/cost-centers/" + costCenterId), "cost_center.in_use");
    }

    @Test
    @DisplayName("cliente com obras deve responder 409 client.in_use")
    void clientWithProjects() {
        String clientId = api.createClient();
        api.create("/api/projects", api.projectBody(api.createCostCenter(), clientId));

        assertConflict(api.delete("/api/clients/" + clientId), "client.in_use");
        assertThat(api.get("/api/clients/" + clientId).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("obra com hospedagens deve responder 409 project.in_use")
    void projectWithAccommodations() {
        String projectId = api.createProject();
        api.create("/api/accommodations", api.accommodationBody(projectId));

        assertConflict(api.delete("/api/projects/" + projectId), "project.in_use");
    }

    @Test
    @DisplayName("colaborador em obra deve responder 409 employee.in_use com mensagem citando obras")
    void employeeInProject() {
        String costCenterId = api.createCostCenter();
        String employeeId = api.create("/api/employees", api.employeeBody(costCenterId));
        Map<String, Object> project = api.projectBody(costCenterId, api.createClient());
        project.put("employeeIds", List.of(employeeId));
        api.create("/api/projects", project);

        ResponseEntity<Map> response = api.delete("/api/employees/" + employeeId);

        assertConflict(response, "employee.in_use");
        assertThat((String) response.getBody().get("message")).contains("projects");
    }

    @Test
    @DisplayName("colaborador em hospedagem deve responder 409 employee.in_use")
    void employeeInAccommodation() {
        String employeeId = api.create("/api/employees", api.employeeBody(api.createCostCenter()));
        Map<String, Object> accommodation = api.accommodationBody(api.createProject());
        accommodation.put("employeeIds", List.of(employeeId));
        api.create("/api/accommodations", accommodation);

        ResponseEntity<Map> response = api.delete("/api/employees/" + employeeId);

        assertConflict(response, "employee.in_use");
        assertThat((String) response.getBody().get("message")).contains("accommodations");
    }

    @Test
    @DisplayName("registro sem dependentes deve ser excluido com 204")
    void recordWithoutDependentsIsDeleted() {
        String clientId = api.createClient();

        assertThat(api.delete("/api/clients/" + clientId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(api.get("/api/clients/" + clientId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Map<String, Object> userBody() {
        return new java.util.HashMap<>(Map.of(
                "name", AdminApi.unique("Usuario "),
                "email", AdminApi.unique("usuario") + "@exemplo.test",
                "password", "senha123",
                "isActive", true,
                "roles", List.of("OPERADOR")));
    }

    private long total(String path) {
        return ((Number) api.get(path + "?size=1").getBody().get("totalElements")).longValue();
    }

    private static void assertConflict(ResponseEntity<Map> response, String errorCode) {
        assertThat(response.getStatusCode()).as(String.valueOf(response.getBody())).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409).containsEntry("errorCode", errorCode);
    }
}
