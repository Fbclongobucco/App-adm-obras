package com.longobuccodev.app_adm_obras.infra.audit;

import com.longobuccodev.app_adm_obras.application.ApplicationFixtures;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.support.AdminApi;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Auditoria de alteracoes (spec change-audit-log), gravada pelo listener do Hibernate. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class ChangeAuditIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    private AdminApi api;
    private Map me;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
        me = api.get("/api/auth/me").getBody();
    }

    @Test
    @DisplayName("Criar cliente deve gerar CLIENT/CREATE com o ADMIN como autor")
    void createShouldBeAudited() {
        String clientId = api.createClient();

        List<Map<String, Object>> events = events("CLIENT", clientId);

        assertThat(events).hasSize(1);
        Map<String, Object> event = events.getFirst();
        assertThat(event.get("action")).isEqualTo("CREATE");
        assertThat(event.get("actor_id")).hasToString((String) me.get("id"));
        assertThat(event.get("actor_name")).isEqualTo(me.get("name"));
        assertThat(event.get("actor_email")).isEqualTo(me.get("email"));
        assertThat(event.get("occurred_at")).isNotNull();
    }

    @Test
    @DisplayName("Alterar obra deve gerar PROJECT/UPDATE")
    void updateShouldBeAudited() {
        String costCenterId = api.createCostCenter();
        String clientId = api.createClient();
        String projectId = api.create("/api/projects", api.projectBody(costCenterId, clientId));
        Map<String, Object> body = api.projectBody(costCenterId, clientId);
        body.put("description", "Descricao alterada");

        assertThat(api.put("/api/projects/" + projectId, body).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(actions("PROJECT", projectId)).containsExactly("CREATE", "UPDATE");
    }

    @Test
    @DisplayName("Excluir restaurante deve gerar RESTAURANT/DELETE, consultavel apos a exclusao")
    void deleteShouldBeAudited() {
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(api.createProject()));

        assertThat(api.delete("/api/restaurants/" + restaurantId).getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(actions("RESTAURANT", restaurantId)).containsExactly("CREATE", "DELETE");
        assertThat(jdbc.queryForObject("select count(*) from restaurant where id = ?", Integer.class,
                UUID.fromString(restaurantId))).isZero();
    }

    @Test
    @DisplayName("Renovar hospedagem deve gerar ACCOMMODATION_PERIOD/CREATE do periodo novo")
    void childCreateShouldBeAudited() {
        String accommodationId = api.create("/api/accommodations", api.accommodationBody(api.createProject()));

        ResponseEntity<Map> renewal = api.post("/api/accommodations/" + accommodationId + "/renewals", Map.of());
        assertThat(renewal.getStatusCode().is2xxSuccessful()).isTrue();

        List<UUID> periods = jdbc.queryForList("select id from accommodation_period where accommodation_id = ? "
                + "order by start_date", UUID.class, UUID.fromString(accommodationId));
        assertThat(periods).hasSize(2);
        assertThat(actions("ACCOMMODATION_PERIOD", periods.getLast().toString())).containsExactly("CREATE");
    }

    @Test
    @DisplayName("Excluir refeicao deve gerar DELETE de cada ajuste, removidos pelo JPA e nao so pelo banco")
    void cascadeDeleteShouldAuditChildren() {
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(api.createProject()));
        String mealId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        for (String date : List.of("2026-01-06", "2026-01-07")) {
            assertThat(api.post("/api/meals/" + mealId + "/adjustments",
                    Map.of("date", date, "type", "NO_SERVICE")).getStatusCode().is2xxSuccessful()).isTrue();
        }
        List<UUID> adjustments = jdbc.queryForList("select id from meal_adjustment where meal_id = ?", UUID.class,
                UUID.fromString(mealId));
        assertThat(adjustments).hasSize(2);

        assertThat(api.delete("/api/meals/" + mealId).getStatusCode().is2xxSuccessful()).isTrue();

        // Cada ajuste toca o updatedAt da refeicao: os UPDATE do meio sao gravacoes reais
        List<String> mealActions = actions("MEAL", mealId);
        assertThat(mealActions.getFirst()).isEqualTo("CREATE");
        assertThat(mealActions.getLast()).isEqualTo("DELETE");
        for (UUID adjustment : adjustments) {
            assertThat(actions("MEAL_ADJUSTMENT", adjustment.toString())).containsExactly("CREATE", "DELETE");
        }
    }

    @Test
    @DisplayName("Gravacao rejeitada por conflito (409) ou validacao (400) nao deve gerar evento")
    void rejectedWriteShouldNotBeAudited() {
        Map<String, Object> duplicate = api.clientBody();
        api.create("/api/clients", duplicate);
        int before = total();

        Map<String, Object> sameEmail = api.clientBody();
        sameEmail.put("email", duplicate.get("email"));
        assertThat(api.post("/api/clients", sameEmail).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Map<String, Object> invalid = api.clientBody();
        invalid.put("email", "sem-arroba");
        assertThat(api.post("/api/clients", invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(total()).isEqualTo(before);
    }

    @Test
    @DisplayName("Leitura nao deve gerar evento")
    void readShouldNotBeAudited() {
        String clientId = api.createClient();
        int before = total();

        api.get("/api/clients/" + clientId);
        api.get("/api/clients?size=5");

        assertThat(total()).isEqualTo(before);
    }

    @Test
    @DisplayName("Alteracao desfeita deve desfazer tambem o evento")
    void rolledBackWriteShouldNotBeAudited() {
        Client client = ApplicationFixtures.client();
        client.markCreated(Instant.now());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            clientRepository.save(client);
            entityManager.flush();
            assertThat(events("CLIENT", client.getId().toString())).as("evento dentro da transacao").hasSize(1);
            status.setRollbackOnly();
        });

        assertThat(events("CLIENT", client.getId().toString())).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from client where id = ?", Integer.class, client.getId()))
                .isZero();
    }

    @Test
    @DisplayName("Admin criado pelo bootstrap deve ter USER/CREATE com autor SYSTEM")
    void bootstrapShouldBeAuditedAsSystem() {
        List<Map<String, Object>> events = events("USER", (String) me.get("id"));

        assertThat(events).isNotEmpty();
        assertThat(events.getFirst()).containsEntry("action", "CREATE").containsEntry("actor_name", "SYSTEM");
        assertThat(events.getFirst().get("actor_id")).isNull();
    }

    @Test
    @DisplayName("Renomear o autor depois nao deve alterar o evento ja gravado")
    void authorRenameShouldNotChangeEvent() {
        String email = AdminApi.unique("autor") + "@exemplo.test";
        Map<String, Object> user = new HashMap<>(Map.of("name", "Autor Original", "email", email, "isActive", true,
                "roles", List.of("ADMIN"), "password", "senha-forte-123"));
        String userId = api.create("/api/users", user);
        AdminApi author = new AdminApi(rest, email, "senha-forte-123");
        String costCenterId = author.createCostCenter();

        user.put("name", "Autor Renomeado");
        user.remove("password");
        assertThat(api.put("/api/users/" + userId, user).getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> event = events("COST_CENTER", costCenterId).getFirst();
        assertThat(event.get("actor_id")).hasToString(userId);
        assertThat(event).containsEntry("actor_name", "Autor Original").containsEntry("actor_email", email);
    }

    @Test
    @DisplayName("Troca de senha gera USER/UPDATE, e o evento nao tem como guardar senha nem valores")
    void passwordChangeShouldNotStoreValues() {
        Map<String, Object> user = new HashMap<>(Map.of("name", "Usuario Senha",
                "email", AdminApi.unique("senha") + "@exemplo.test", "isActive", true, "roles", List.of("OPERADOR"),
                "password", "senha-forte-123"));
        String userId = api.create("/api/users", user);
        user.put("password", "outra-senha-456");

        assertThat(api.put("/api/users/" + userId, user).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(actions("USER", userId)).containsExactly("CREATE", "UPDATE");
        assertThat(jdbc.queryForList("select column_name from information_schema.columns "
                + "where table_name = 'change_audit'", String.class))
                .containsExactlyInAnyOrder("id", "entity_type", "entity_id", "action", "occurred_at",
                        "actor_id", "actor_name", "actor_email");
    }

    private List<Map<String, Object>> events(String type, String entityId) {
        return jdbc.queryForList("select * from change_audit where entity_type = ? and entity_id = ? "
                + "order by occurred_at, action", type, UUID.fromString(entityId));
    }

    private List<String> actions(String type, String entityId) {
        return events(type, entityId).stream().map(event -> (String) event.get("action")).toList();
    }

    private int total() {
        return jdbc.queryForObject("select count(*) from change_audit", Integer.class);
    }
}
