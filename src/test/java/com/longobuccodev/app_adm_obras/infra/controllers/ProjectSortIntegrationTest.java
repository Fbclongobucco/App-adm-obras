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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class ProjectSortIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    @Test
    @DisplayName("sort=updatedAt,desc deve trazer as obras da alteracao mais recente para a mais antiga")
    void shouldListMostRecentlyUpdatedFirst() {
        for (int i = 0; i < 3; i++) {
            api.createProject();
        }

        ResponseEntity<Map> response = api.get("/api/projects?sort=updatedAt,desc&size=10");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map> content = (List<Map>) response.getBody().get("content");
        assertThat(content).hasSizeGreaterThanOrEqualTo(3).hasSizeLessThanOrEqualTo(10);
        List<Instant> updated = content.stream().map(p -> Instant.parse((String) p.get("updatedAt"))).toList();
        assertThat(updated).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    @DisplayName("obra alterada deve passar para o topo da ordenacao por updatedAt,desc")
    void updatedProjectShouldMoveToTop() {
        String costCenterId = api.createCostCenter();
        String clientId = api.createClient();
        Map<String, Object> body = api.projectBody(costCenterId, clientId);
        String projectId = api.create("/api/projects", body);
        api.createProject();

        body.put("description", "Obra alterada");
        api.put("/api/projects/" + projectId, body);

        List<Map> content = (List<Map>) api.get("/api/projects?sort=updatedAt,desc&size=1").getBody().get("content");
        assertThat(content.get(0).get("id")).isEqualTo(projectId);
    }

    @Test
    @DisplayName("sem direcao a ordenacao deve ser crescente")
    void directionShouldDefaultToAscending() {
        api.createProject();

        List<Map> content = (List<Map>) api.get("/api/projects?sort=os&size=100").getBody().get("content");

        List<String> os = content.stream().map(p -> (String) p.get("os")).toList();
        assertThat(os).isSorted();
    }

    @Test
    @DisplayName("campo ou direcao nao permitidos devem responder 400 page.invalid.sort")
    void shouldRejectUnsupportedSort() {
        for (String sort : List.of("totalPrice,desc", "updatedAt,up")) {
            ResponseEntity<Map> response = api.get("/api/projects?sort=" + sort);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("errorCode", "page.invalid.sort");
        }
    }

    @Test
    @DisplayName("empates em updatedAt nao devem repetir nem pular obras entre paginas")
    void tiesShouldNotRepeatAcrossPages() {
        List<String> tied = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tied.add(api.createProject());
        }
        Timestamp same = Timestamp.from(Instant.parse("2020-06-01T12:00:00Z"));
        for (String id : tied) {
            jdbc.update("update project set created_at = ?, updated_at = ? where id = ?", same, same, UUID.fromString(id));
        }

        List<String> seen = new ArrayList<>();
        long total;
        int page = 0;
        do {
            Map body = api.get("/api/projects?sort=updatedAt,desc&size=2&page=" + page).getBody();
            ((List<Map>) body.get("content")).forEach(p -> seen.add((String) p.get("id")));
            total = ((Number) body.get("totalElements")).longValue();
            page++;
        } while (seen.size() < total && page < 1000);

        assertThat(seen).doesNotHaveDuplicates().hasSize((int) total).containsAll(tied);
    }

    @Test
    @DisplayName("requisicao sem sort deve continuar respondendo 200 com a estrutura de pagina")
    void requestWithoutSortShouldKeepWorking() {
        ResponseEntity<Map> response = api.get("/api/projects?page=0&size=5");

        assertThat(response.getStatusCode()).as(String.valueOf(response.getBody())).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("content", "page", "size", "totalElements", "totalPages");
    }
}
