package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.core.domain.User.Role;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    static final String ADMIN_EMAIL = "admin@admin.com";
    static final String ADMIN_PASSWORD = "admin123";
    static final String FRONTEND_ORIGIN = "http://localhost:3000";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    // --- login ---

    @Test
    @DisplayName("login deve devolver access token, perfil e o refresh token so no cookie")
    void loginShouldReturnTokensAndProfile() {
        ResponseEntity<Map> response = postLogin(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("tokenType", "Bearer").containsEntry("expiresIn", 900);
        assertThat((String) body.get("accessToken")).isNotBlank();
        Map<String, Object> user = userOf(body);
        assertThat(user).containsEntry("email", ADMIN_EMAIL);
        assertThat((List<String>) user.get("roles")).containsExactly("ADMIN");

        String refreshToken = refreshTokenOf(response);
        assertThat(refreshToken).isNotBlank();
        assertThat(body.toString()).doesNotContain(refreshToken).doesNotContain("password").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("cookie padrao deve ser HttpOnly, SameSite=Strict, Path=/api/auth, sem Domain e com Max-Age de 7 dias")
    void defaultCookieAttributes() {
        String cookie = refreshCookieHeader(postLogin(ADMIN_EMAIL, ADMIN_PASSWORD));

        assertThat(cookie).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api/auth")
                .doesNotContainIgnoringCase("Domain=")
                .doesNotContain("Partitioned");
        // perfil test roda sem TLS
        assertThat(cookie).doesNotContain("Secure");
        // Max-Age e o tempo restante ate a expiracao de 7 dias, com folga do tempo de processamento
        assertThat(maxAgeOf(cookie)).isBetween(Duration.ofDays(7).toSeconds() - 5, Duration.ofDays(7).toSeconds());
    }

    @Test
    @DisplayName("senha errada e email inexistente devem ter a mesma resposta 401, sem o email")
    void invalidCredentialsShouldBeUniform() {
        ResponseEntity<Map> wrongPassword = postLogin(ADMIN_EMAIL, "senha-errada");
        ResponseEntity<Map> unknownEmail = postLogin("ninguem@admin.com", ADMIN_PASSWORD);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getBody().get("errorCode")).isEqualTo(wrongPassword.getBody().get("errorCode"));
        assertThat(unknownEmail.getBody().get("message")).isEqualTo(wrongPassword.getBody().get("message"));
        assertThat(unknownEmail.getBody().toString()).doesNotContain("ninguem@admin.com");
        assertThat(wrongPassword.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("login sem corpo ou sem senha deve responder 401")
    void loginWithoutCredentialsShouldBeUnauthorized() {
        assertThat(restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(null, jsonHeaders()), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(Map.of("email", ADMIN_EMAIL),
                jsonHeaders()), Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("usuario inativo nao deve conseguir logar")
    void inactiveUserCannotLogin() {
        createUser("Usuario Inativo", "inativo.login@admin.com", false, Role.OPERADOR, "senha123");

        assertThat(postLogin("inativo.login@admin.com", "senha123").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- access token ---

    @Test
    @DisplayName("Bearer valido deve acessar rota protegida")
    void validBearerShouldAccessProtectedRoute() {
        ResponseEntity<Map> response = get("/api/users", accessToken(ADMIN_EMAIL, ADMIN_PASSWORD));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("content", "totalElements", "first", "last");
    }

    @Test
    @DisplayName("rota protegida sem token deve responder 401")
    void missingTokenShouldBeUnauthorized() {
        assertThat(restTemplate.getForEntity("/api/users", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("token com payload alterado deve responder 401")
    void tamperedTokenShouldBeUnauthorized() {
        String[] parts = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"ADMIN\"", "\"OPERADOR\"");
        String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        assertThat(get("/api/users", forged).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("token expirado deve responder 401")
    void expiredTokenShouldBeUnauthorized() {
        // Token assinado com a chave real, emitido e expirado no passado
        Instant issuedAt = Instant.now().minus(Duration.ofHours(1));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("app-adm-obras")
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
                .claim("roles", List.of("ADMIN"))
                .build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();

        assertThat(get("/api/users", expired).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("HTTP Basic com credenciais corretas deve responder 401")
    void basicCredentialsShouldBeRefused() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(restTemplate.exchange("/api/users", HttpMethod.GET, new HttpEntity<>(headers), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("nenhuma resposta deve criar sessao HTTP")
    void noSessionCookieShouldBeIssued() {
        ResponseEntity<Map> login = postLogin(ADMIN_EMAIL, ADMIN_PASSWORD);
        ResponseEntity<Map> me = get("/api/auth/me", (String) login.getBody().get("accessToken"));
        ResponseEntity<String> anonymous = restTemplate.getForEntity("/api/users", String.class);

        for (ResponseEntity<?> response : List.of(login, me, anonymous)) {
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            assertThat(cookies == null ? List.of() : cookies).noneMatch(c -> c.startsWith("JSESSIONID"));
        }
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("OPERADOR com token valido nao deve escrever")
    void operatorCannotWrite() {
        createUser("Operador Escrita", "operador.escrita@admin.com", true, Role.OPERADOR, "senha123");
        String token = accessToken("operador.escrita@admin.com", "senha123");

        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange("/api/users", HttpMethod.POST,
                new HttpEntity<>("{}", headers), String.class);

        assertThat(get("/api/users", token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("/api/auth/me deve devolver o perfil do dono do token")
    void meShouldReturnProfile() {
        ResponseEntity<Map> response = get("/api/auth/me", accessToken(ADMIN_EMAIL, ADMIN_PASSWORD));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", ADMIN_EMAIL).containsKeys("id", "name", "roles");
    }

    @Test
    @DisplayName("/api/auth/me sem token deve responder 401")
    void meWithoutTokenShouldBeUnauthorized() {
        assertThat(restTemplate.getForEntity("/api/auth/me", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- refresh e logout ---

    @Test
    @DisplayName("refresh deve devolver novo access token e trocar o cookie")
    void refreshShouldRotate() {
        String refreshToken = refreshTokenOf(postLogin(ADMIN_EMAIL, ADMIN_PASSWORD));

        ResponseEntity<Map> refreshed = postWithCookie("/api/auth/refresh", refreshToken, FRONTEND_ORIGIN);

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) refreshed.getBody().get("accessToken")).isNotBlank();
        String next = refreshTokenOf(refreshed);
        assertThat(next).isNotBlank().isNotEqualTo(refreshToken);
        assertThat(refreshed.getBody().toString()).doesNotContain(next);
        assertThat(get("/api/users", (String) refreshed.getBody().get("accessToken")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("reapresentar refresh token trocado deve revogar tambem o mais novo")
    void replayShouldRevokeChain() {
        String first = refreshTokenOf(postLogin(ADMIN_EMAIL, ADMIN_PASSWORD));
        String second = refreshTokenOf(postWithCookie("/api/auth/refresh", first, FRONTEND_ORIGIN));

        ResponseEntity<Map> replay = postWithCookie("/api/auth/refresh", first, FRONTEND_ORIGIN);
        ResponseEntity<Map> afterReplay = postWithCookie("/api/auth/refresh", second, FRONTEND_ORIGIN);

        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(replay.getBody()).containsEntry("errorCode", "auth.invalid.refresh");
        assertThat(afterReplay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("refresh sem cookie deve responder 401")
    void refreshWithoutCookieShouldBeUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(FRONTEND_ORIGIN);

        assertThat(restTemplate.exchange("/api/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("refresh recusado deve apagar o cookie com os mesmos atributos")
    void refusedRefreshShouldClearCookie() {
        ResponseEntity<Map> response = postWithCookie("/api/auth/refresh", "valor-desconhecido", FRONTEND_ORIGIN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refreshCookieHeader(response)).startsWith("refresh_token=;").contains("Max-Age=0")
                .contains("Path=/api/auth").contains("HttpOnly").contains("SameSite=Strict");
    }

    @Test
    @DisplayName("logout deve responder 204, apagar o cookie e impedir o refresh")
    void logoutShouldRevoke() {
        String refreshToken = refreshTokenOf(postLogin(ADMIN_EMAIL, ADMIN_PASSWORD));

        ResponseEntity<Map> logout = postWithCookie("/api/auth/logout", refreshToken, FRONTEND_ORIGIN);

        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logout.getBody()).isNull();
        assertThat(refreshCookieHeader(logout)).startsWith("refresh_token=;").contains("Max-Age=0")
                .contains("Path=/api/auth").contains("HttpOnly").contains("SameSite=Strict");
        assertThat(postWithCookie("/api/auth/refresh", refreshToken, FRONTEND_ORIGIN).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("logout sem cookie ou com token desconhecido deve responder 204")
    void logoutWithUnknownTokenIsSilent() {
        assertThat(postWithCookie("/api/auth/logout", "desconhecido", FRONTEND_ORIGIN).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(FRONTEND_ORIGIN);
        assertThat(restTemplate.exchange("/api/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("access token continua valido apos o logout ate expirar")
    void accessTokenSurvivesLogout() {
        ResponseEntity<Map> login = postLogin(ADMIN_EMAIL, ADMIN_PASSWORD);
        postWithCookie("/api/auth/logout", refreshTokenOf(login), FRONTEND_ORIGIN);

        assertThat(get("/api/users", (String) login.getBody().get("accessToken")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("refresh e logout de origem nao permitida ou sem Origin devem responder 403 sem efeito")
    void foreignOriginShouldBeForbidden() {
        String refreshToken = refreshTokenOf(postLogin(ADMIN_EMAIL, ADMIN_PASSWORD));

        ResponseEntity<String> foreignRefresh = postWithCookieRaw("/api/auth/refresh", refreshToken,
                "https://site-malicioso.example.com");
        ResponseEntity<String> foreignLogout = postWithCookieRaw("/api/auth/logout", refreshToken,
                "https://site-malicioso.example.com");
        ResponseEntity<String> noOrigin = postWithCookieRaw("/api/auth/refresh", refreshToken, null);

        assertThat(foreignRefresh.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(foreignLogout.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(noOrigin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(foreignRefresh.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
        // o token nao foi consumido nem revogado
        assertThat(postWithCookie("/api/auth/refresh", refreshToken, FRONTEND_ORIGIN).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    // --- usuarios e senha ---

    @Test
    @DisplayName("troca de senha via API deve invalidar o refresh token anterior")
    void passwordChangeShouldInvalidateRefreshToken() {
        Map<String, Object> created = createUser("Troca Senha", "troca.senha@admin.com", true, Role.OPERADOR,
                "senhaAntiga1").getBody();
        String oldRefresh = refreshTokenOf(postLogin("troca.senha@admin.com", "senhaAntiga1"));

        HttpHeaders headers = bearer(accessToken(ADMIN_EMAIL, ADMIN_PASSWORD));
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> updated = restTemplate.exchange("/api/users/" + created.get("id"), HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "Troca Senha", "email", "troca.senha@admin.com", "isActive", true,
                        "roles", List.of("OPERADOR"), "password", "senhaNova12"), headers), Map.class);

        assertThat(updated.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(postWithCookie("/api/auth/refresh", oldRefresh, FRONTEND_ORIGIN).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postLogin("troca.senha@admin.com", "senhaNova12").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("usuario criado pela API deve conseguir logar e a senha nunca volta em resposta")
    void createdUserCanLoginAndPasswordIsNeverReturned() {
        Map<String, Object> created = createUser("Novo Operador", "operador.novo@admin.com", true, Role.OPERADOR,
                "secreta123").getBody();
        assertThat(created).doesNotContainKeys("password", "passwordHash");

        ResponseEntity<Map> login = postLogin("operador.novo@admin.com", "secreta123");
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<String>) userOf(login.getBody()).get("roles")).containsExactly("OPERADOR");

        Map<String, Object> listed = get("/api/users", accessToken(ADMIN_EMAIL, ADMIN_PASSWORD)).getBody();
        assertThat(listed.toString()).doesNotContain("secreta123").doesNotContain("$2a$");
    }

    // --- CORS ---

    @Test
    @DisplayName("CORS deve liberar o frontend Next.js com credenciais")
    void corsShouldAllowNextJsOrigin() {
        HttpHeaders headers = bearer(accessToken(ADMIN_EMAIL, ADMIN_PASSWORD));
        headers.setOrigin(FRONTEND_ORIGIN);

        ResponseEntity<Map> response = restTemplate.exchange("/api/users", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(FRONTEND_ORIGIN);
        assertThat(response.getHeaders().getAccessControlAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("CORS preflight para refresh deve ser respondido")
    void corsPreflightShouldBeAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(FRONTEND_ORIGIN);
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(List.of("content-type", "authorization"));

        ResponseEntity<Map> response = restTemplate.exchange("/api/auth/refresh", HttpMethod.OPTIONS,
                new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(FRONTEND_ORIGIN);
    }

    @Test
    @DisplayName("CORS de origem nao liberada nao deve receber cabecalho de permissao")
    void corsShouldNotAllowUnknownOrigin() {
        HttpHeaders headers = bearer(accessToken(ADMIN_EMAIL, ADMIN_PASSWORD));
        headers.setOrigin("http://origem-malvada.example.com");

        ResponseEntity<String> response = restTemplate.exchange("/api/users", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    // --- helpers ---

    private ResponseEntity<Map> createUser(String name, String email, boolean active, Role role, String password) {
        HttpHeaders headers = bearer(accessToken(ADMIN_EMAIL, ADMIN_PASSWORD));
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> created = restTemplate.exchange("/api/users", HttpMethod.POST, new HttpEntity<>(
                Map.of("name", name, "email", email, "isActive", active, "roles", List.of(role.name()),
                        "password", password), headers), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created;
    }

    private String accessToken(String email, String password) {
        ResponseEntity<Map> login = postLogin(email, password);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) login.getBody().get("accessToken");
    }

    private ResponseEntity<Map> postLogin(String email, String password) {
        return restTemplate.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders()), Map.class);
    }

    private ResponseEntity<Map> postWithCookie(String path, String refreshToken, String origin) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(cookieHeaders(refreshToken, origin)),
                Map.class);
    }

    private ResponseEntity<String> postWithCookieRaw(String path, String refreshToken, String origin) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(cookieHeaders(refreshToken, origin)),
                String.class);
    }

    private static HttpHeaders cookieHeaders(String refreshToken, String origin) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, AuthController.REFRESH_COOKIE + "=" + refreshToken);
        if (origin != null) {
            headers.setOrigin(origin);
        }
        return headers;
    }

    private ResponseEntity<Map> get(String path, String accessToken) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(accessToken)), Map.class);
    }

    private static HttpHeaders bearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    static String refreshCookieHeader(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).as("Set-Cookie").isNotNull();
        return cookies.stream().filter(c -> c.startsWith(AuthController.REFRESH_COOKIE + "="))
                .findFirst().orElseThrow();
    }

    static long maxAgeOf(String cookieHeader) {
        String attribute = cookieHeader.substring(cookieHeader.indexOf("Max-Age=") + "Max-Age=".length());
        return Long.parseLong(attribute.substring(0, attribute.indexOf(';')));
    }

    static String refreshTokenOf(ResponseEntity<?> response) {
        String header = refreshCookieHeader(response);
        return header.substring(header.indexOf('=') + 1, header.indexOf(';'));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> userOf(Map<String, Object> body) {
        return (Map<String, Object>) body.get("user");
    }
}
