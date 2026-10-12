package com.longobuccodev.app_adm_obras.infra.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("descricao OpenAPI 3 deve ser publicada sem credenciais")
    void apiDocsShouldBeServedAnonymously() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/v3/api-docs", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) response.getBody().get("openapi")).startsWith("3.");
    }

    @Test
    @DisplayName("toda rota sob /api deve aparecer na descricao com o mesmo metodo")
    @SuppressWarnings("unchecked")
    void everyApiRouteShouldBeDescribed() {
        Map<String, Map<String, Object>> paths =
                (Map<String, Map<String, Object>>) restTemplate.getForObject("/v3/api-docs", Map.class).get("paths");

        Set<String> declared = new TreeSet<>();
        handlerMapping.getHandlerMethods().keySet().forEach(info ->
                info.getPathPatternsCondition().getPatternValues().stream()
                        .filter(path -> path.startsWith("/api"))
                        .forEach(path -> info.getMethodsCondition().getMethods().forEach(method ->
                                declared.add(method.name().toLowerCase() + " " + path))));

        Set<String> described = new TreeSet<>();
        paths.forEach((path, operations) -> operations.keySet().forEach(method ->
                described.add(method + " " + path)));

        assertThat(declared).isNotEmpty();
        assertThat(described).containsAll(declared);
    }

    @Test
    @DisplayName("descricao deve declarar o esquema Bearer e nao o Basic")
    @SuppressWarnings("unchecked")
    void apiDocsShouldDeclareBearerAuth() {
        Map<String, Object> body = restTemplate.getForObject("/v3/api-docs", Map.class);
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        Map<String, Map<String, Object>> schemes =
                (Map<String, Map<String, Object>>) components.get("securitySchemes");

        assertThat(schemes).containsOnlyKeys(OpenApiConfig.BEARER_AUTH);
        assertThat(schemes.get(OpenApiConfig.BEARER_AUTH))
                .containsEntry("type", "http")
                .containsEntry("scheme", "bearer")
                .containsEntry("bearerFormat", "JWT");
        assertThat(schemes.values()).noneMatch(scheme -> "basic".equals(scheme.get("scheme")));
    }

    @Test
    @DisplayName("Swagger UI deve ser entregue sem credenciais")
    void swaggerUiShouldBeServedAnonymously() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui/index.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
    }

    @Test
    @DisplayName("rotas da API continuam exigindo autenticacao")
    void apiRoutesShouldStillRequireAuthentication() {
        assertThat(restTemplate.getForEntity("/api/clients", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("ADMIN com o token do login deve ler rota da API")
    void apiRoutesShouldAcceptValidToken() {
        assertThat(restTemplate.exchange("/api/clients", HttpMethod.GET,
                new HttpEntity<>(bearer(login("admin@admin.com", "admin123"))), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("OPERADOR com token nao deve executar escrita restrita a ADMIN")
    void operatorTokenShouldNotWrite() {
        HttpHeaders adminHeaders = bearer(login("admin@admin.com", "admin123"));
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange("/api/users", HttpMethod.POST, new HttpEntity<>(Map.of(
                "name", "Operador Docs", "email", "operador.docs@admin.com", "isActive", true,
                "roles", List.of("OPERADOR"), "password", "senha123"), adminHeaders), Map.class);

        HttpHeaders operatorHeaders = bearer(login("operador.docs@admin.com", "senha123"));
        operatorHeaders.setContentType(MediaType.APPLICATION_JSON);

        assertThat(restTemplate.exchange("/api/clients", HttpMethod.POST,
                new HttpEntity<>("{}", operatorHeaders), String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("accessToken");
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    @DisplayName("listagem de obras deve documentar o parametro sort com os campos permitidos")
    @SuppressWarnings("unchecked")
    void projectListingShouldDocumentSort() {
        Map<String, Object> paths = (Map<String, Object>) restTemplate.getForObject("/v3/api-docs", Map.class)
                .get("paths");
        Map<String, Object> get = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/projects")).get("get");
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) get.get("parameters");

        Map<String, Object> sort = parameters.stream()
                .filter(parameter -> "sort".equals(parameter.get("name")))
                .findFirst().orElseThrow();
        assertThat(sort).containsEntry("required", false);
        assertThat((String) sort.get("description")).contains("updatedAt", "createdAt", "os", "startDate");
    }

    @Test
    @DisplayName("fechamento semanal e ajustes do restaurante devem estar documentados")
    @SuppressWarnings("unchecked")
    void weeklyClosingShouldBeDocumented() {
        Map<String, Object> paths = (Map<String, Object>) restTemplate.getForObject("/v3/api-docs", Map.class)
                .get("paths");
        for (String path : List.of("/api/meals/{id}/weekly-closing", "/api/restaurants/{id}/weekly-closing")) {
            Map<String, Object> get = (Map<String, Object>) ((Map<String, Object>) paths.get(path)).get("get");
            assertThat((String) get.get("description")).contains("OUT_OF_PERIOD", "selected");
        }
        assertThat((Map<String, Object>) paths.get("/api/restaurants/{id}/adjustments")).containsKey("post");
        assertThat((Map<String, Object>) paths.get("/api/restaurants/{id}/adjustments/{date}")).containsKey("delete");
    }

    @Test
    @DisplayName("listagens devem documentar o parametro de busca q")
    @SuppressWarnings("unchecked")
    void listingsShouldDocumentSearch() {
        Map<String, Object> paths = (Map<String, Object>) restTemplate.getForObject("/v3/api-docs", Map.class)
                .get("paths");
        for (String path : List.of("/api/projects", "/api/clients", "/api/employees", "/api/cost-centers",
                "/api/restaurants", "/api/meals", "/api/accommodations", "/api/users")) {
            Map<String, Object> get = (Map<String, Object>) ((Map<String, Object>) paths.get(path)).get("get");
            Map<String, Object> q = ((List<Map<String, Object>>) get.get("parameters")).stream()
                    .filter(parameter -> "q".equals(parameter.get("name")))
                    .findFirst().orElseThrow(() -> new AssertionError(path + " sem parametro q"));
            assertThat(q).containsEntry("required", false);
            assertThat((String) q.get("description")).contains("page.invalid.query");
        }
    }

    @Test
    @DisplayName("PUT de obra deve documentar a substituicao completa e que refeicoes seguem o restaurante")
    @SuppressWarnings("unchecked")
    void projectUpdateShouldDocumentReplacementSemantics() {
        Map<String, Object> paths = (Map<String, Object>) restTemplate.getForObject("/v3/api-docs", Map.class)
                .get("paths");
        Map<String, Object> put = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/projects/{id}"))
                .get("put");

        assertThat((String) put.get("description"))
                .contains("employeeIds", "accommodationIds", "restaurante", "meals")
                .doesNotContain("lunchId", "dinnerId");
    }

    @Test
    @DisplayName("POST e PUT de refeicao devem documentar tipos, campos por refeicao servida e custos")
    @SuppressWarnings("unchecked")
    void mealWriteShouldDocumentContract() {
        Map<String, Object> paths = (Map<String, Object>) restTemplate.getForObject("/v3/api-docs", Map.class)
                .get("paths");
        Map<String, Object> post = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/meals")).get("post");
        Map<String, Object> put = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/meals/{id}"))
                .get("put");

        for (Map<String, Object> operation : java.util.List.of(post, put)) {
            assertThat((String) operation.get("description")).contains("LUNCH_AND_DINNER",
                    "meal.invalid.lunch_quantity", "meal.invalid.dinner_price", "meal.duplicate.type",
                    "dailyPrice", "workingDays", "totalPrice");
        }
    }
}
