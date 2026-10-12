package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.support.AdminApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Locacao mensal da hospedagem pela API (change hospedagem-locacao-mensal). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class AccommodationLeaseIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;
    private String projectId;
    private Map<String, Object> body;
    private String id;

    /** Entrada em 2026-10-12, 1500.00 por mes (AdminApi.accommodationBody). */
    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
        projectId = api.createProject();
        body = api.accommodationBody(projectId);
        id = api.create("/api/accommodations", body);
    }

    private String path() {
        return "/api/accommodations/" + id;
    }

    private static void assertMoney(Object actual, String expected) {
        assertThat(new BigDecimal(actual.toString())).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("criacao: primeiro periodo, termino, total e sugestao de renovacao; sem days")
    void createdLease() {
        Map accommodation = api.get(path()).getBody();

        assertThat(accommodation).containsEntry("startDate", "2026-10-12").containsEntry("endDate", "2026-11-11")
                .doesNotContainKey("days");
        assertMoney(accommodation.get("monthlyPrice"), "1500.00");
        assertMoney(accommodation.get("totalPrice"), "1500.00");
        assertThat((List<Map>) accommodation.get("periods")).singleElement()
                .satisfies(period -> assertThat(period).containsEntry("days", 31).containsEntry("endDate", "2026-11-11"));
        assertThat((Map) accommodation.get("nextRenewal")).containsEntry("startDate", "2026-11-12")
                .containsEntry("endDate", "2026-12-11");
    }

    @Test
    @DisplayName("renovar com sugestao e com outro valor, desfazer, e total da obra acompanha")
    void renewAndUndo() {
        ResponseEntity<Map> suggested = api.post(path() + "/renewals", null);
        ResponseEntity<Map> custom = api.post(path() + "/renewals", Map.of("endDate", "2026-12-26", "price", 800));

        assertThat(suggested.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(custom.getBody()).containsEntry("endDate", "2026-12-26");
        assertMoney(custom.getBody().get("totalPrice"), "3800.00");
        assertThat((List) custom.getBody().get("periods")).hasSize(3);
        assertMoney(api.get("/api/projects/" + projectId).getBody().get("totalPrice"), "3800.00");

        assertThat(api.delete(path() + "/renewals/last").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(api.get(path()).getBody()).containsEntry("endDate", "2026-12-11");
        api.delete(path() + "/renewals/last");
        ResponseEntity<Map> first = api.delete(path() + "/renewals/last");
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(first.getBody()).containsEntry("errorCode", "accommodation.lease.first_period");
    }

    @Test
    @DisplayName("renovacao invalida: 400 com o codigo e nada muda")
    void invalidRenewal() {
        ResponseEntity<Map> end = api.post(path() + "/renewals", Map.of("endDate", "2026-11-01"));
        ResponseEntity<Map> price = api.post(path() + "/renewals", Map.of("price", 0));

        assertThat(end.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(end.getBody()).containsEntry("errorCode", "accommodation.invalid.renewal_end");
        assertThat(price.getBody()).containsEntry("errorCode", "accommodation.invalid.renewal_price");
        assertThat((List) api.get(path()).getBody().get("periods")).hasSize(1);
    }

    @Test
    @DisplayName("PUT: sem renovacao muda a entrada; com renovacao a entrada responde 409 e os periodos ficam")
    void editLease() {
        body.put("startDate", "2026-10-15");
        body.put("monthlyPrice", 1600);
        ResponseEntity<Map> moved = api.put(path(), body);
        assertThat(moved.getBody()).containsEntry("startDate", "2026-10-15").containsEntry("endDate", "2026-11-14");
        assertMoney(moved.getBody().get("totalPrice"), "1600.00");

        api.post(path() + "/renewals", null);
        body.put("startDate", "2026-10-20");
        ResponseEntity<Map> blocked = api.put(path(), body);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(blocked.getBody()).containsEntry("errorCode", "accommodation.lease.renewed");

        body.put("startDate", "2026-10-15");
        body.put("capacity", 6);
        ResponseEntity<Map> kept = api.put(path(), body);
        assertThat(kept.getBody()).containsEntry("capacity", 6);
        assertThat((List) kept.getBody().get("periods")).hasSize(2);
    }

    @Test
    @DisplayName("sem entrada ou valor mensal: 400")
    void missingLeaseFields() {
        Map<String, Object> invalid = api.accommodationBody(projectId);
        invalid.remove("monthlyPrice");
        ResponseEntity<Map> response = api.post("/api/accommodations", invalid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("errorCode", "accommodation.invalid.monthly_price");
    }

    @Test
    @DisplayName("OPERADOR nao renova nem desfaz (403)")
    void operatorIsReadOnly() {
        String email = AdminApi.unique("operador") + "@exemplo.test";
        api.create("/api/users", Map.of("name", "Operador Locacao", "email", email, "password", "senha123",
                "isActive", true, "roles", List.of("OPERADOR")));
        String token = (String) rest.postForEntity("/api/auth/login", Map.of("email", email, "password", "senha123"),
                Map.class).getBody().get("accessToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        assertThat(rest.exchange(path() + "/renewals", HttpMethod.POST, new HttpEntity<>(Map.of(), headers), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rest.exchange(path() + "/renewals/last", HttpMethod.DELETE, new HttpEntity<>(headers), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
