package com.longobuccodev.app_adm_obras.infra.persistence;

import com.longobuccodev.app_adm_obras.support.AdminApi;
import com.longobuccodev.app_adm_obras.support.QueryCounter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custo de consulta das leituras e escritas (spec persistence-query-efficiency). Banco H2 proprio: a massa
 * e uniforme (toda obra tem cliente, centro de custo, 2 colaboradores, hospedagem, restaurante e almoco),
 * entao a contagem so pode variar com o tamanho da pagina ou das colecoes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        QueryCounter.PROPERTY,
        "spring.datasource.url=jdbc:h2:mem:query-count;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"})
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"rawtypes", "unchecked"})
class QueryCountIntegrationTest {

    private static final int RECORDS = 50;
    private static final int SMALL = 2;
    private static final int LARGE = 30;

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;
    private String costCenterId;
    private final List<String> spareEmployees = new ArrayList<>();

    @BeforeAll
    void seed() {
        api = new AdminApi(rest);
        costCenterId = api.createCostCenter();
        for (int i = 0; i < LARGE * 2; i++) {
            spareEmployees.add(api.create("/api/employees", api.employeeBody(costCenterId)));
        }
        for (int i = 0; i < RECORDS; i++) {
            createUniformProject(SMALL);
            Map<String, Object> user = Map.of("name", AdminApi.unique("Operador "),
                    "email", AdminApi.unique("operador") + "@exemplo.test", "isActive", true,
                    "roles", List.of("OPERADOR"), "password", "senha-forte-123");
            api.create("/api/users", new HashMap<>(user));
        }
    }

    @Test
    @DisplayName("Leitura por id deve contar ao menos uma consulta")
    void countsReadById() {
        QueryCounter.Result result = QueryCounter.count(() -> ok(api.get("/api/cost-centers/" + costCenterId)));

        assertThat(result.count()).isGreaterThanOrEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "/api/projects?",
            "/api/projects?q=obra&",
            "/api/clients?",
            "/api/clients?q=cliente&",
            "/api/employees?",
            "/api/employees?q=colaborador&",
            "/api/cost-centers?",
            "/api/accommodations?",
            "/api/meals?",
            "/api/restaurants?",
            "/api/users?"})
    @DisplayName("Listagem deve executar o mesmo numero de consultas para pagina 5 e pagina 50")
    void listingDoesNotGrowWithPageSize(String path) {
        QueryCounter.Result small = QueryCounter.count(() -> ok(api.get(path + "size=5")));
        QueryCounter.Result large = QueryCounter.count(() -> ok(api.get(path + "size=" + RECORDS)));

        assertSameCount(path, "size=5", small, "size=" + RECORDS, large);
    }

    @Test
    @DisplayName("Detalhe da obra nao deve crescer com o numero de colaboradores")
    void projectDetailIsBounded() {
        String few = createUniformProject(SMALL);
        String many = createUniformProject(LARGE);

        assertDetailBounded("/api/projects/", few, many);
    }

    @Test
    @DisplayName("Detalhe da hospedagem nao deve crescer com colaboradores nem periodos")
    void accommodationDetailIsBounded() {
        String projectId = createUniformProject(SMALL);
        String few = createAccommodation(projectId, SMALL);
        String many = createAccommodation(projectId, LARGE);
        for (int i = 0; i < 3; i++) {
            ok(api.post("/api/accommodations/" + many + "/renewals", Map.of()));
        }

        assertDetailBounded("/api/accommodations/", few, many);
    }

    @Test
    @DisplayName("Detalhe da refeicao nao deve crescer com o numero de ajustes")
    void mealDetailIsBounded() {
        String few = mealOf(createUniformProject(SMALL));
        String many = mealOf(createUniformProject(SMALL));
        // Dias uteis a partir do inicio da obra (segunda-feira, 2026-01-05)
        for (String date : List.of("2026-01-06", "2026-01-07", "2026-01-08", "2026-01-09", "2026-01-12")) {
            ok(api.post("/api/meals/" + many + "/adjustments", Map.of("date", date, "type", "NO_SERVICE")));
        }

        assertDetailBounded("/api/meals/", few, many);
    }

    @Test
    @DisplayName("Detalhe do cliente nao deve crescer com o numero de obras")
    void clientDetailIsBounded() {
        String few = api.createClient();
        String many = api.createClient();
        api.create("/api/projects", api.projectBody(costCenterId, few));
        for (int i = 0; i < 10; i++) {
            api.create("/api/projects", api.projectBody(costCenterId, many));
        }

        assertDetailBounded("/api/clients/", few, many);
    }

    @Test
    @DisplayName("Gravar obra nao deve fazer uma leitura por colaborador vinculado")
    void projectWriteIsBounded() {
        // Cliente e colaboradores novos, sem outras obras: os dois casos so diferem na quantidade
        Map<String, Object> few = projectWithEmployees(api.createClient(), newEmployees(SMALL));
        Map<String, Object> many = projectWithEmployees(api.createClient(), newEmployees(LARGE));
        QueryCounter.Result small = QueryCounter.count(() -> api.create("/api/projects", few));
        QueryCounter.Result large = QueryCounter.count(() -> api.create("/api/projects", many));

        assertSameCount("POST /api/projects", SMALL + " colaboradores", small, LARGE + " colaboradores", large);
    }

    @Test
    @DisplayName("Gravar hospedagem nao deve fazer uma leitura por colaborador vinculado")
    void accommodationWriteIsBounded() {
        String projectId = createUniformProject(SMALL);
        QueryCounter.Result small = QueryCounter.count(() -> createAccommodation(projectId, SMALL));
        QueryCounter.Result large = QueryCounter.count(() -> createAccommodation(projectId, LARGE));

        assertSameCount("POST /api/accommodations", SMALL + " colaboradores", small, LARGE + " colaboradores", large);
    }

    @Test
    @DisplayName("Listagem e detalhe da mesma obra devem devolver o mesmo conteudo")
    void listingAndDetailAgree() {
        String projectId = createUniformProject(SMALL);
        Map detail = ok(api.get("/api/projects/" + projectId)).getBody();

        List<Map> content = (List<Map>) ok(api.get("/api/projects?size=5&q=" + detail.get("os"))).getBody()
                .get("content");

        assertThat(content).hasSize(1);
        assertThat(withSetsSorted(content.getFirst())).isEqualTo(withSetsSorted(detail));
        assertThat((List<?>) detail.get("employees")).hasSize(SMALL);
        assertThat((List<?>) detail.get("meals")).hasSize(1);
        assertThat((List<?>) detail.get("restaurants")).hasSize(1);
        assertThat((List<?>) detail.get("accommodations")).hasSize(1);
    }

    // --- massa ---

    /** Obra com cliente e centro de custo proprios, colaboradores, hospedagem, restaurante e almoco. */
    private String createUniformProject(int employees) {
        String ownCostCenter = api.createCostCenter();
        List<String> employeeIds = new ArrayList<>();
        for (int i = 0; i < employees; i++) {
            employeeIds.add(api.create("/api/employees", api.employeeBody(ownCostCenter)));
        }
        Map<String, Object> project = api.projectBody(ownCostCenter, api.createClient());
        project.put("employeeIds", employeeIds);
        String projectId = api.create("/api/projects", project);
        createAccommodation(projectId, SMALL);
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(projectId));
        api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        return projectId;
    }

    private String createAccommodation(String projectId, int employees) {
        Map<String, Object> body = api.accommodationBody(projectId);
        body.put("employeeIds", spareEmployees.subList(0, employees));
        return api.create("/api/accommodations", body);
    }

    private Map<String, Object> projectWithEmployees(String clientId, List<String> employeeIds) {
        Map<String, Object> body = api.projectBody(costCenterId, clientId);
        body.put("employeeIds", employeeIds);
        return body;
    }

    private List<String> newEmployees(int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(api.create("/api/employees", api.employeeBody(costCenterId)));
        }
        return ids;
    }

    private String mealOf(String projectId) {
        List<Map> meals = (List<Map>) ok(api.get("/api/projects/" + projectId)).getBody().get("meals");
        return (String) meals.getFirst().get("id");
    }

    // --- verificacao ---

    /** employees e accommodations sao Set na API: a ordem nao faz parte do conteudo. */
    private static Map withSetsSorted(Map project) {
        Map copy = new HashMap(project);
        for (String field : List.of("employees", "accommodations")) {
            List<Map> items = new ArrayList<>((List<Map>) copy.get(field));
            items.sort(Comparator.comparing(item -> (String) item.get("id")));
            copy.put(field, items);
        }
        return copy;
    }

    private void assertDetailBounded(String path, String few, String many) {
        QueryCounter.Result small = QueryCounter.count(() -> ok(api.get(path + few)));
        QueryCounter.Result large = QueryCounter.count(() -> ok(api.get(path + many)));

        assertSameCount("GET " + path + "{id}", "poucos vinculos", small, "muitos vinculos", large);
    }

    private static void assertSameCount(String endpoint, String smallLabel, QueryCounter.Result small,
                                        String largeLabel, QueryCounter.Result large) {
        // Linha de consulta para perf/RESULTADOS.md
        System.out.printf("QUERY-COUNT\t%s\t%s=%d\t%s=%d%n", endpoint, smallLabel, small.count(),
                largeLabel, large.count());
        assertThat(large.count())
                .as("%s: %s -> %s%n%s -> %s", endpoint, smallLabel, small, largeLabel, large)
                .isEqualTo(small.count());
    }

    private static <T extends ResponseEntity<?>> T ok(T response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).as("resposta %s", response).isTrue();
        return response;
    }
}
