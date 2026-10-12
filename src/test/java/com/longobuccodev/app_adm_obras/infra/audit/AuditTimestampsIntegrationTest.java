package com.longobuccodev.app_adm_obras.infra.audit;

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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class AuditTimestampsIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    @Test
    @DisplayName("registro criado deve trazer criacao e alteracao iguais")
    void createdRecordShouldCarryEqualTimestamps() {
        ResponseEntity<Map> response = api.post("/api/cost-centers", api.costCenterBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("createdAt")).isNotNull();
        assertThat(response.getBody().get("updatedAt")).isEqualTo(response.getBody().get("createdAt"));
    }

    @Test
    @DisplayName("datas enviadas pelo cliente devem ser ignoradas, e a alteracao deve avancar so updatedAt")
    void clientTimestampsShouldBeIgnoredAndUpdateShouldAdvance() throws InterruptedException {
        Map<String, Object> body = api.clientBody();
        body.put("createdAt", "2000-01-01T00:00:00Z");
        body.put("updatedAt", "2000-01-01T00:00:00Z");
        ResponseEntity<Map> created = api.post("/api/clients", body);
        Instant createdAt = instant(created, "createdAt");
        assertThat(createdAt).isAfter(Instant.parse("2020-01-01T00:00:00Z"));

        Thread.sleep(5);
        body.put("name", AdminApi.unique("Cliente Alterado "));
        ResponseEntity<Map> updated = api.put("/api/clients/" + AdminApi.id(created), body);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(instant(updated, "createdAt")).isEqualTo(createdAt);
        assertThat(instant(updated, "updatedAt")).isAfter(createdAt);
        ResponseEntity<Map> read = api.get("/api/clients/" + AdminApi.id(created));
        assertThat(instant(read, "createdAt")).isEqualTo(createdAt);
        assertThat(instant(read, "updatedAt")).isEqualTo(instant(updated, "updatedAt"));
    }

    @Test
    @DisplayName("alteracao rejeitada deve manter a data de alteracao")
    void rejectedUpdateShouldKeepTimestamp() {
        Map<String, Object> body = api.costCenterBody();
        ResponseEntity<Map> created = api.post("/api/cost-centers", body);

        body.put("cnpj", "123");
        ResponseEntity<Map> rejected = api.put("/api/cost-centers/" + AdminApi.id(created), body);

        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<Map> read = api.get("/api/cost-centers/" + AdminApi.id(created));
        assertThat(read.getBody().get("updatedAt")).isEqualTo(created.getBody().get("updatedAt"));
    }

    @Test
    @DisplayName("listagem e detalhe devem devolver as mesmas datas")
    void listingAndDetailShouldMatch() {
        String costCenterId = api.createCostCenter();
        ResponseEntity<Map> employee = api.post("/api/employees", api.employeeBody(costCenterId));

        List<Map> listed = api.getList("/api/employees/by-cost-center/" + costCenterId).getBody();
        ResponseEntity<Map> detail = api.get("/api/employees/" + AdminApi.id(employee));

        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).get("createdAt")).isEqualTo(detail.getBody().get("createdAt"));
        assertThat(listed.get(0).get("updatedAt")).isEqualTo(detail.getBody().get("updatedAt"));
    }

    @Test
    @DisplayName("criar, alterar e excluir hospedagem nao deve mudar a data de alteracao da obra")
    void accommodationChangesShouldNotTouchProject() {
        String projectId = api.createProject();
        Object projectUpdatedAt = api.get("/api/projects/" + projectId).getBody().get("updatedAt");

        Map<String, Object> body = api.accommodationBody(projectId);
        ResponseEntity<Map> accommodation = api.post("/api/accommodations", body);
        body.put("days", 12);
        api.put("/api/accommodations/" + AdminApi.id(accommodation), body);
        api.delete("/api/accommodations/" + AdminApi.id(accommodation));

        assertThat(api.get("/api/projects/" + projectId).getBody().get("updatedAt")).isEqualTo(projectUpdatedAt);
    }

    private static Instant instant(ResponseEntity<Map> response, String field) {
        return Instant.parse((String) response.getBody().get(field));
    }
}
