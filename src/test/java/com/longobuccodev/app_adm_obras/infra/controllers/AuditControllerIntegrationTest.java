package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.support.AdminApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** GET /api/audit/changes e /api/audit/logins (specs change-audit-log e login-audit-log). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class AuditControllerIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    @Test
    @DisplayName("Historico de um registro deve trazer so os eventos dele, do mais recente para o mais antigo")
    void shouldListHistoryOfOneRecord() {
        String costCenterId = api.createCostCenter();
        Map<String, Object> body = api.costCenterBody();
        assertThat(api.put("/api/cost-centers/" + costCenterId, body).getStatusCode()).isEqualTo(HttpStatus.OK);
        api.createCostCenter();

        List<Map> content = content(api.get("/api/audit/changes?entityType=COST_CENTER&entityId=" + costCenterId));

        assertThat(content).extracting(item -> item.get("action")).containsExactly("UPDATE", "CREATE");
        assertThat(content).allSatisfy(item -> {
            assertThat(item).containsEntry("entityType", "COST_CENTER").containsEntry("entityId", costCenterId);
            assertThat(item).containsKeys("id", "occurredAt", "actorId", "actorName", "actorEmail");
        });
        List<Instant> instants = content.stream().map(item -> Instant.parse((String) item.get("occurredAt"))).toList();
        assertThat(instants).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    @DisplayName("Alteracoes de um autor em um periodo devem respeitar from inclusivo e to exclusivo")
    void shouldFilterByActorAndPeriod() {
        String email = AdminApi.unique("auditor") + "@exemplo.test";
        String authorId = api.create("/api/users", new HashMap<>(Map.of("name", "Autor Auditado", "email", email,
                "isActive", true, "roles", List.of("ADMIN"), "password", "senha-forte-123")));
        AdminApi author = new AdminApi(rest, email, "senha-forte-123");
        author.createCostCenter();
        List<Map> all = content(api.get("/api/audit/changes?actorId=" + authorId));
        assertThat(all).hasSize(1);
        String at = (String) all.getFirst().get("occurredAt");

        assertThat(content(api.get("/api/audit/changes?actorId=" + authorId + "&from=" + at))).hasSize(1);
        assertThat(content(api.get("/api/audit/changes?actorId=" + authorId + "&to=" + at))).isEmpty();
        assertThat(content(api.get("/api/audit/changes?actorId=" + authorId + "&from=" + at + "&to=" + at)))
                .isEmpty();
    }

    @Test
    @DisplayName("Logins de um usuario e de um periodo devem ser filtrados")
    void shouldFilterLogins() {
        String email = AdminApi.unique("logado") + "@exemplo.test";
        String userId = api.create("/api/users", new HashMap<>(Map.of("name", "Usuario Logado", "email", email,
                "isActive", true, "roles", List.of("OPERADOR"), "password", "senha-forte-123")));
        Instant before = Instant.now().minusSeconds(1);
        new AdminApi(rest, email, "senha-forte-123").get("/api/auth/me");

        List<Map> logins = content(api.get("/api/audit/logins?userId=" + userId));
        assertThat(logins).singleElement().satisfies(item -> {
            assertThat(item).containsEntry("userId", userId).containsEntry("userEmail", email);
            assertThat(item).containsKeys("id", "userName", "occurredAt", "ipAddress", "userAgent");
        });
        assertThat(content(api.get("/api/audit/logins?userId=" + userId + "&from=" + before))).hasSize(1);
        assertThat(content(api.get("/api/audit/logins?userId=" + userId + "&to=" + before))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/audit/changes?entityType=OBRA",
            "/api/audit/changes?from=2026-10-02T00:00:00Z&to=2026-10-01T00:00:00Z",
            "/api/audit/logins?from=2026-10-02T00:00:00Z&to=2026-10-01T00:00:00Z",
            "/api/audit/changes?from=ontem",
            "/api/audit/logins?to=2026-13-01",
            "/api/audit/changes?size=500"})
    @DisplayName("Filtro invalido deve responder 400 com errorCode")
    void invalidFilterShouldRespond400(String path) {
        ResponseEntity<Map> response = api.get(path);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errorCode");
        assertThat((String) response.getBody().get("errorCode")).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/audit/changes", "/api/audit/logins"})
    @DisplayName("OPERADOR deve receber 403 e requisicao sem token 401")
    void onlyAdminShouldRead(String path) {
        String email = AdminApi.unique("operador") + "@exemplo.test";
        api.create("/api/users", new HashMap<>(Map.of("name", "Operador Curioso", "email", email, "isActive", true,
                "roles", List.of("OPERADOR"), "password", "senha-forte-123")));

        assertThat(new AdminApi(rest, email, "senha-forte-123").get(path).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rest.getForEntity(path, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.get(path).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static List<Map> content(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).as("resposta %s", response).isEqualTo(HttpStatus.OK);
        return (List<Map>) response.getBody().get("content");
    }
}
