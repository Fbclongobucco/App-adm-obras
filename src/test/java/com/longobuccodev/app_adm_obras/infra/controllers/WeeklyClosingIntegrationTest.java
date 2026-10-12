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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Fechamento semanal e ajustes no restaurante (change fechamento-semanal-refeicoes). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class WeeklyClosingIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;
    private String restaurantId;
    private String lunchId;
    private String dinnerId;

    /** Obra de 2026-10-05 a 2026-10-16, restaurante com taxa 8.00 e refrigerante 5.00, Almoco e Jantar separados. */
    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
        Map<String, Object> project = api.projectBody(api.createCostCenter(), api.createClient());
        project.put("startDate", "2026-10-05");
        project.put("endDate", "2026-10-16");
        Map<String, Object> restaurant = api.restaurantBody(api.create("/api/projects", project));
        restaurant.put("deliveryFee", 8);
        restaurant.put("sodaPrice", 5);
        restaurantId = api.create("/api/restaurants", restaurant);
        lunchId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        dinnerId = api.create("/api/meals", api.mealBody(restaurantId, "DINNER"));
    }

    private String restaurantPath() {
        return "/api/restaurants/" + restaurantId;
    }

    private static Map<String, Object> adjustment(String date, String type, List<Map<String, Object>> quantities) {
        Map<String, Object> body = new HashMap<>();
        body.put("date", date);
        body.put("type", type);
        body.put("note", "Teste");
        body.put("quantities", quantities);
        return body;
    }

    private static Map<String, Object> quantity(String mealId, Integer lunch, Integer dinner) {
        Map<String, Object> body = new HashMap<>();
        body.put("mealId", mealId);
        body.put("lunchQuantity", lunch);
        body.put("dinnerQuantity", dinner);
        return body;
    }

    private static void assertMoney(Object actual, String expected) {
        assertThat(new BigDecimal(actual.toString())).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("GET do restaurante: semanas, semana pedida e soma das refeicoes por dia")
    void restaurantWeeklyClosing() {
        Map closing = api.get(restaurantPath() + "/weekly-closing?week=2026-10-07").getBody();

        assertThat((List<Map>) closing.get("weeks")).extracting(week -> week.get("start"))
                .containsExactly("2026-10-05", "2026-10-12");
        assertThat(closing.get("selected")).isEqualTo("2026-10-05");
        List<Map> days = (List<Map>) closing.get("days");
        assertThat(days).hasSize(7);
        assertThat(days.getFirst()).containsEntry("date", "2026-10-05").containsEntry("weekday", "MONDAY")
                .containsEntry("status", "SERVICE").containsEntry("lunchMeals", 10).containsEntry("dinnerMeals", 8)
                .containsEntry("deliveries", 2).containsEntry("sodas", 2);
        assertMoney(days.getFirst().get("price"), "452.00");
        assertThat(days.get(5)).containsEntry("status", "OFF");
        Map totals = (Map) closing.get("totals");
        assertThat(totals).containsEntry("serviceDays", 5);
        assertMoney(totals.get("price"), "2260.00");
    }

    @Test
    @DisplayName("GET da refeicao com semana fora da lista usa a semana padrao")
    void mealWeeklyClosingFallsBackToDefault() {
        Map closing = api.get("/api/meals/" + lunchId + "/weekly-closing?week=2030-01-01").getBody();

        assertThat(closing.get("selected")).isIn("2026-10-05", "2026-10-12");
        assertThat(((List<Map>) closing.get("weeks")).get(1)).containsEntry("end", "2026-10-16")
                .containsEntry("partial", true);
        assertThat(api.get("/api/meals/" + lunchId + "/weekly-closing").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("feriado no restaurante grava nas 2 refeicoes; DELETE remove das 2; segundo DELETE 404")
    void holidayOnAllMeals() {
        ResponseEntity<List> created = api.postList(restaurantPath() + "/adjustments",
                adjustment("2026-10-12", "NO_SERVICE", null));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).hasSize(2);
        Map monday = ((List<Map>) api.get(restaurantPath() + "/weekly-closing?week=2026-10-12").getBody()
                .get("days")).getFirst();
        assertThat(monday).containsEntry("status", "NO_SERVICE");
        assertThat((List) monday.get("adjustmentIds")).hasSize(2);
        assertThat((List) api.get("/api/meals/" + dinnerId).getBody().get("adjustments")).hasSize(1);

        assertThat(api.delete(restaurantPath() + "/adjustments/2026-10-12").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat((List) api.get("/api/meals/" + lunchId).getBody().get("adjustments")).isEmpty();
        assertThat((List) api.get("/api/meals/" + dinnerId).getBody().get("adjustments")).isEmpty();
        ResponseEntity<Map> again = api.delete(restaurantPath() + "/adjustments/2026-10-12");
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(again.getBody()).containsEntry("errorCode", "meal.adjustment.not_found");
    }

    @Test
    @DisplayName("conflito em uma refeicao: 409 e nenhuma refeicao recebe o ajuste")
    void conflictWritesNothing() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("date", "2026-10-12");
        existing.put("type", "NO_SERVICE");
        api.create("/api/meals/" + dinnerId + "/adjustments", existing);

        ResponseEntity<Map> response = api.post(restaurantPath() + "/adjustments",
                adjustment("2026-10-12", "NO_SERVICE", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("errorCode", "meal.adjustment.duplicate.date");
        assertThat((List) api.get("/api/meals/" + lunchId).getBody().get("adjustments")).isEmpty();
    }

    @Test
    @DisplayName("dia extra so nas refeicoes com quantidade; sem quantidade 400; refeicao de outro restaurante 404")
    void extraDayOnlyWithQuantity() {
        ResponseEntity<List> created = api.postList(restaurantPath() + "/adjustments", adjustment("2026-10-10",
                "EXTRA_DAY", List.of(quantity(lunchId, 6, null), quantity(dinnerId, null, 0))));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).singleElement().satisfies(item -> assertMoney(((Map) item).get("price"), "163.00"));
        assertThat((List) api.get("/api/meals/" + dinnerId).getBody().get("adjustments")).isEmpty();
        Map saturday = ((List<Map>) api.get(restaurantPath() + "/weekly-closing?week=2026-10-10").getBody()
                .get("days")).get(5);
        assertThat(saturday).containsEntry("status", "EXTRA_DAY").containsEntry("lunchMeals", 6);

        ResponseEntity<Map> none = api.post(restaurantPath() + "/adjustments",
                adjustment("2026-10-11", "EXTRA_DAY", List.of(quantity(lunchId, 0, null))));
        assertThat(none.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(none.getBody()).containsEntry("errorCode", "meal.adjustment.invalid.quantity");

        String otherRestaurant = api.create("/api/restaurants", api.restaurantBody(api.createProject()));
        String otherMeal = api.create("/api/meals", api.mealBody(otherRestaurant, "LUNCH"));
        assertThat(api.post(restaurantPath() + "/adjustments",
                adjustment("2026-10-11", "EXTRA_DAY", List.of(quantity(otherMeal, 5, null)))).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("OPERADOR le o fechamento, mas nao inclui nem remove ajustes do restaurante (403)")
    void operatorIsReadOnly() {
        String email = AdminApi.unique("operador") + "@exemplo.test";
        api.create("/api/users", Map.of("name", "Operador Fechamento", "email", email, "password", "senha123",
                "isActive", true, "roles", List.of("OPERADOR")));
        String token = (String) rest.postForEntity("/api/auth/login", Map.of("email", email, "password", "senha123"),
                Map.class).getBody().get("accessToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        assertThat(rest.exchange(restaurantPath() + "/weekly-closing", HttpMethod.GET, new HttpEntity<>(headers),
                Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rest.exchange(restaurantPath() + "/adjustments", HttpMethod.POST,
                new HttpEntity<>(adjustment("2026-10-12", "NO_SERVICE", null), headers), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rest.exchange(restaurantPath() + "/adjustments/2026-10-12", HttpMethod.DELETE,
                new HttpEntity<>(headers), Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
