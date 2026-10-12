package com.longobuccodev.app_adm_obras.support;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cliente HTTP autenticado como o administrador do perfil test, com fabricas de
 * requisicoes validas e unicas (o banco H2 e compartilhado entre as classes de teste).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AdminApi {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.nanoTime());

    private final TestRestTemplate rest;
    private final FakeDocuments documents = new FakeDocuments(SEQUENCE.incrementAndGet());
    private String token;

    public AdminApi(TestRestTemplate rest) {
        this.rest = rest;
    }

    public ResponseEntity<Map> get(String path) {
        return exchange(HttpMethod.GET, path, null);
    }

    public ResponseEntity<List> getList(String path) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers()), List.class);
    }

    public ResponseEntity<Map> post(String path, Object body) {
        return exchange(HttpMethod.POST, path, body);
    }

    public ResponseEntity<Map> put(String path, Object body) {
        return exchange(HttpMethod.PUT, path, body);
    }

    public ResponseEntity<Map> delete(String path) {
        return exchange(HttpMethod.DELETE, path, null);
    }

    public ResponseEntity<Map> exchange(HttpMethod method, String path, Object body) {
        return rest.exchange(path, method, new HttpEntity<>(body, headers()), Map.class);
    }

    // --- fabricas de corpo de requisicao ---

    public static String unique(String prefix) {
        return prefix + SEQUENCE.incrementAndGet();
    }

    public static Map<String, Object> address() {
        Map<String, Object> address = new HashMap<>();
        address.put("street", "Rua das Palmeiras");
        address.put("number", "120");
        address.put("city", "Campinas");
        address.put("state", "SP");
        address.put("country", "Brasil");
        address.put("neighborhood", "Centro");
        address.put("zipCode", "13010-000");
        return address;
    }

    public Map<String, Object> costCenterBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", unique("Centro "));
        body.put("cnpj", documents.nextCnpj());
        return body;
    }

    public Map<String, Object> clientBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", unique("Cliente "));
        body.put("email", unique("cliente") + "@exemplo.test");
        body.put("phone", "1933334444");
        body.put("address", address());
        return body;
    }

    public Map<String, Object> employeeBody(String costCenterId) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", unique("Colaborador "));
        body.put("email", unique("colaborador") + "@exemplo.test");
        body.put("cpf", documents.nextCpf());
        body.put("phone", "19988887777");
        body.put("birthDate", "1990-05-10");
        body.put("costCenterId", costCenterId);
        body.put("role", "MONTADOR");
        return body;
    }

    public Map<String, Object> projectBody(String costCenterId, String clientId) {
        Map<String, Object> body = new HashMap<>();
        body.put("os", unique("OS-"));
        body.put("description", "Obra de teste");
        body.put("costCenterId", costCenterId);
        body.put("clientId", clientId);
        body.put("startDate", "2026-01-05");
        body.put("isCompleted", false);
        return body;
    }

    public Map<String, Object> accommodationBody(String projectId) {
        Map<String, Object> body = new HashMap<>();
        body.put("hostName", unique("Pousada "));
        body.put("hostPhone", "1933335555");
        body.put("address", address());
        body.put("capacity", 4);
        body.put("days", 10);
        body.put("isContract", true);
        body.put("projectId", projectId);
        body.put("totalPrice", 1500);
        return body;
    }

    public Map<String, Object> restaurantBody(String projectId) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", unique("Restaurante "));
        body.put("phone", "1933336666");
        body.put("address", address());
        body.put("projectId", projectId);
        return body;
    }

    public Map<String, Object> mealBody(String restaurantId, String mealType) {
        Map<String, Object> body = new HashMap<>();
        body.put("restaurantId", restaurantId);
        body.put("price", 25);
        body.put("isBilled", false);
        body.put("mealType", mealType);
        body.put("quantity", 10);
        body.put("date", "2026-01-15");
        return body;
    }

    // --- criacao com verificacao ---

    public String create(String path, Map<String, Object> body) {
        ResponseEntity<Map> response = post(path, body);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("POST " + path + " falhou: " + response.getStatusCode() + " "
                    + response.getBody());
        }
        return (String) response.getBody().get("id");
    }

    public String createCostCenter() {
        return create("/api/cost-centers", costCenterBody());
    }

    public String createClient() {
        return create("/api/clients", clientBody());
    }

    public String createProject() {
        return create("/api/projects", projectBody(createCostCenter(), createClient()));
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token());
        return headers;
    }

    private String token() {
        if (token == null) {
            ResponseEntity<Map> login = rest.postForEntity("/api/auth/login",
                    Map.of("email", "admin@admin.com", "password", "admin123"), Map.class);
            token = (String) login.getBody().get("accessToken");
        }
        return token;
    }

    public static UUID id(ResponseEntity<Map> response) {
        return UUID.fromString((String) response.getBody().get("id"));
    }
}
