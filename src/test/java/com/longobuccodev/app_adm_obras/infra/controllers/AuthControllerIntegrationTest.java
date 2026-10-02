package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("login com o usuario bootstrap deve retornar perfil e role ADMIN")
    void loginWithBootstrapUserShouldSucceed() {
        ResponseEntity<Map> response = postLogin("admin@admim.com", "admin123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", "admin@admim.com");
        assertThat(rolesOf(response.getBody())).containsExactly("ADMIN");
        assertThat(response.getBody()).doesNotContainKey("password");
    }

    @Test
    @DisplayName("login com senha errada deve retornar 401 sem revelar se o email existe")
    void loginWithWrongPasswordShouldBeUnauthorized() {
        ResponseEntity<Map> response = postLogin("admin@admim.com", "senha-errada");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("errorCode", "auth.invalid.credentials");
    }

    @Test
    @DisplayName("login com email inexistente deve retornar 401")
    void loginWithUnknownEmailShouldBeUnauthorized() {
        assertThat(postLogin("ninguem@admim.com", "admin123").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login sem credenciais deve retornar 401")
    void loginWithoutBodyShouldBeUnauthorized() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", Map.of(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("/api/auth/me deve retornar o perfil autenticado")
    void meShouldReturnAuthenticatedProfile() {
        HttpHeaders headers = basicAuth("admin@admim.com", "admin123");

        ResponseEntity<Map> response = restTemplate.exchange("/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", "admin@admim.com");
    }

    @Test
    @DisplayName("/api/auth/me sem credenciais deve retornar 401")
    void meWithoutCredentialsShouldBeUnauthorized() {
        assertThat(restTemplate.getForEntity("/api/auth/me", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("o bootstrap deve permitir listar recursos com HTTP Basic")
    void basicAuthShouldAccessProtectedResources() {
        HttpHeaders headers = basicAuth("admin@admim.com", "admin123");

        ResponseEntity<Map> response = restTemplate.exchange("/api/users", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("content", "totalElements", "first", "last");
    }

    @Test
    @DisplayName("CORS deve liberar o frontend Next.js em localhost:3000")
    void corsShouldAllowNextJsOrigin() {
        HttpHeaders headers = basicAuth("admin@admim.com", "admin123");
        headers.setOrigin("http://localhost:3000");
        headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

        ResponseEntity<Map> response = restTemplate.exchange("/api/users", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("http://localhost:3000");
    }

    @Test
    @DisplayName("CORS preflight para escrita deve ser respondido")
    void corsPreflightShouldBeAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:3000");
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(java.util.List.of("content-type"));

        ResponseEntity<Map> response = restTemplate.exchange("/api/users", HttpMethod.OPTIONS,
                new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("http://localhost:3000");
    }

    @Test
    @DisplayName("CORS de origem nao liberada nao deve receber cabecalho de permissao")
    void corsShouldNotAllowUnknownOrigin() {
        HttpHeaders headers = basicAuth("admin@admim.com", "admin123");
        headers.setOrigin("http://origem-malvada.example.com");

        ResponseEntity<String> response = restTemplate.exchange("/api/users", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    @DisplayName("um ADMIN criado pela API deve conseguir autenticar com a senha definida")
    void createdAdminShouldBeAbleToAuthenticate() {
        HttpHeaders adminHeaders = basicAuth("admin@admim.com", "admin123");
        Map<String, Object> body = Map.of(
                "name", "Novo Operador",
                "email", "operador.novo@admim.com",
                "isActive", true,
                "roles", java.util.List.of(Role.OPERADOR.name()),
                "password", "senha123");

        ResponseEntity<Map> created = restTemplate.exchange("/api/users", HttpMethod.POST,
                new HttpEntity<>(body, adminHeaders), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).doesNotContainKey("password");

        ResponseEntity<Map> login = postLogin("operador.novo@admim.com", "senha123");
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rolesOf(login.getBody())).containsExactly("OPERADOR");
    }

    @Test
    @DisplayName("a senha nunca deve voltar em resposta, nem em create nem em listagem")
    void passwordShouldNeverBeReturned() {
        HttpHeaders adminHeaders = basicAuth("admin@admim.com", "admin123");

        Map<String, Object> created = restTemplate.exchange("/api/users", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Sem Expor", "email", "sem.expor@admim.com",
                        "isActive", true, "roles", java.util.List.of("OPERADOR"), "password", "secreta123"),
                        adminHeaders), Map.class).getBody();

        assertThat(created).doesNotContainKey("password");
        assertThat(created).doesNotContainKey("passwordHash");

        Map<String, Object> listed = restTemplate.exchange("/api/users", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), Map.class).getBody();
        assertThat(listed.toString()).doesNotContain("secreta123").doesNotContain("$2a$");
    }

    @SuppressWarnings("unchecked")
    private java.util.List<String> rolesOf(Map<String, Object> body) {
        return (java.util.List<String>) body.get("roles");
    }

    private ResponseEntity<Map> postLogin(String email, String password) {
        return restTemplate.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders()), Map.class);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders basicAuth(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        String token = java.util.Base64.getEncoder()
                .encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
        return headers;
    }
}
