package com.longobuccodev.app_adm_obras.infra.controllers;

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

import java.util.Map;

import static com.longobuccodev.app_adm_obras.infra.controllers.AuthControllerIntegrationTest.ADMIN_EMAIL;
import static com.longobuccodev.app_adm_obras.infra.controllers.AuthControllerIntegrationTest.ADMIN_PASSWORD;
import static com.longobuccodev.app_adm_obras.infra.controllers.AuthControllerIntegrationTest.FRONTEND_ORIGIN;
import static com.longobuccodev.app_adm_obras.infra.controllers.AuthControllerIntegrationTest.refreshCookieHeader;
import static org.assertj.core.api.Assertions.assertThat;

/** Topologia C: frontend e API em sites diferentes, sem proxy. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.refresh-cookie.same-site=None",
        "app.security.refresh-cookie.secure=true",
        "app.security.refresh-cookie.partitioned=true"
})
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class CrossSiteRefreshCookieIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("com SameSite=None, o cookie deve sair Secure e Partitioned")
    void crossSiteCookieAttributes() {
        String cookie = refreshCookieHeader(login());

        assertThat(cookie).contains("SameSite=None").contains("Secure").contains("Partitioned")
                .contains("HttpOnly").contains("Path=/api/auth");
    }

    @Test
    @DisplayName("o cookie de remocao deve repetir SameSite, Secure e Partitioned")
    void clearCookieKeepsAttributes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(FRONTEND_ORIGIN);

        ResponseEntity<String> logout = restTemplate.exchange("/api/auth/logout", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);

        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(refreshCookieHeader(logout)).contains("Max-Age=0").contains("SameSite=None")
                .contains("Secure").contains("Partitioned").contains("Path=/api/auth");
    }

    private ResponseEntity<Map> login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD), headers), Map.class);
    }
}
