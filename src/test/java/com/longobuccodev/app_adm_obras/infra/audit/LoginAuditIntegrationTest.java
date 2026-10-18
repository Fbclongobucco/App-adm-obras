package com.longobuccodev.app_adm_obras.infra.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Auditoria de login (spec login-audit-log) pela API real. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class LoginAuditIntegrationTest {

    private static final String EMAIL = "admin@admin.com";
    private static final String PASSWORD = "admin123";
    // Nome do cookie de AuthController.REFRESH_COOKIE (visibilidade de pacote)
    private static final String REFRESH_COOKIE = "refresh_token";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Login com sucesso deve registrar usuario, IP e user-agent da requisicao")
    void successfulLoginShouldBeRecorded() {
        ResponseEntity<Map> login = login(PASSWORD, "Navegador/1.0 (teste)");
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID userId = UUID.fromString((String) ((Map) login.getBody().get("user")).get("id"));

        Map<String, Object> record = latest(userId);

        assertThat(record).containsEntry("user_email", EMAIL).containsEntry("user_agent", "Navegador/1.0 (teste)");
        assertThat((String) record.get("ip_address")).isIn("127.0.0.1", "0:0:0:0:0:0:0:1");
        assertThat(record.get("user_name")).isNotNull();
    }

    @Test
    @DisplayName("Login rejeitado nao deve gerar registro")
    void failedLoginShouldNotBeRecorded() {
        int before = total();

        assertThat(login("senha-errada", "Navegador/1.0").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(total()).isEqualTo(before);
    }

    @Test
    @DisplayName("Login sem User-Agent deve funcionar e registrar user-agent vazio")
    void loginWithoutUserAgentShouldWork() throws Exception {
        // MockMvc: o cliente HTTP do Java sempre envia o proprio User-Agent
        int before = total();
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());

        assertThat(total()).isEqualTo(before + 1);
        Map<String, Object> record = jdbc.queryForList(
                "select * from login_audit order by occurred_at desc, id desc").getFirst();
        assertThat(record).containsEntry("user_email", EMAIL);
        assertThat(record.get("user_agent")).isNull();
    }

    @Test
    @DisplayName("User-agent longo deve ser truncado em 512, sem rejeitar o login")
    void longUserAgentShouldBeTruncated() {
        ResponseEntity<Map> login = login(PASSWORD, "x".repeat(2000));
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID userId = UUID.fromString((String) ((Map) login.getBody().get("user")).get("id"));

        assertThat((String) latest(userId).get("user_agent")).hasSize(512);
    }

    @Test
    @DisplayName("Refresh e logout nao devem gerar registro de login")
    void refreshAndLogoutShouldNotBeRecorded() {
        String refreshToken = refreshTokenOf(login(PASSWORD, "Navegador/1.0"));
        int before = total();

        ResponseEntity<Map> refreshed = postWithCookie("/api/auth/refresh", refreshToken);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postWithCookie("/api/auth/logout", refreshTokenOf(refreshed)).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(total()).isEqualTo(before);
    }

    private ResponseEntity<Map> login(String password, String userAgent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (userAgent != null) {
            headers.set(HttpHeaders.USER_AGENT, userAgent);
        }
        return rest.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", EMAIL, "password", password), headers), Map.class);
    }

    private ResponseEntity<Map> postWithCookie(String path, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, REFRESH_COOKIE + "=" + refreshToken);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    }

    private static String refreshTokenOf(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        String cookie = cookies.stream().filter(c -> c.startsWith(REFRESH_COOKIE + "=")).findFirst()
                .orElseThrow();
        return cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));
    }

    private Map<String, Object> latest(UUID userId) {
        return jdbc.queryForList("select * from login_audit where user_id = ? order by occurred_at desc, id desc",
                userId).getFirst();
    }

    private int total() {
        return jdbc.queryForObject("select count(*) from login_audit", Integer.class);
    }
}
