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

/** Ajustes por data da refeicao pela API (change refeicao-ajustes-por-data). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class MealAdjustmentIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;
    private Map<String, Object> projectBody;
    private String projectId;
    private String mealId;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
        // Obra de 2026-10-05 a 2026-10-16; LUNCH_AND_DINNER 10 x 25 e 8 x 22; taxa 8 e refrigerante 5: 452/dia
        projectBody = api.projectBody(api.createCostCenter(), api.createClient());
        projectBody.put("startDate", "2026-10-05");
        projectBody.put("endDate", "2026-10-16");
        projectId = api.create("/api/projects", projectBody);
        Map<String, Object> restaurant = api.restaurantBody(projectId);
        restaurant.put("lunchDeliveryFee", 8);
        restaurant.put("dinnerDeliveryFee", 8);
        restaurant.put("sodaPrice", 5);
        String restaurantId = api.create("/api/restaurants", restaurant);
        mealId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH_AND_DINNER"));
    }

    private String path() {
        return "/api/meals/" + mealId + "/adjustments";
    }

    private static Map<String, Object> adjustment(String date, String type, Integer lunch, Integer dinner,
                                                  String note) {
        Map<String, Object> body = new HashMap<>();
        body.put("date", date);
        body.put("type", type);
        body.put("lunchQuantity", lunch);
        body.put("dinnerQuantity", dinner);
        body.put("note", note);
        return body;
    }

    @Test
    @DisplayName("cenario da spec: 201, lista por data, detalhe com campos novos e total da obra 4231,00")
    void specScenario() {
        ResponseEntity<Map> holiday = api.post(path(),
                adjustment("2026-10-12", "NO_SERVICE", null, null, "Feriado de N. Sra. Aparecida"));
        ResponseEntity<Map> extra = api.post(path(), adjustment("2026-10-10", "EXTRA_DAY", 6, 0, null));

        assertThat(holiday.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(extra.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(extra.getBody()).containsEntry("date", "2026-10-10").containsEntry("weekday", "SATURDAY")
                .containsEntry("type", "EXTRA_DAY").containsEntry("lunchQuantity", 6)
                .containsEntry("dinnerQuantity", 0).containsEntry("deliveries", 1).containsEntry("sodas", 1)
                .containsEntry("inPeriod", true).containsKey("id").containsKey("note");
        assertMoney(extra.getBody().get("price"), "163.00");
        assertThat(holiday.getBody()).containsEntry("weekday", "MONDAY").containsEntry("deliveries", 0)
                .containsEntry("note", "Feriado de N. Sra. Aparecida");
        assertMoney(holiday.getBody().get("price"), "0");

        List<Map> listed = api.getList(path()).getBody();
        assertThat(listed).extracting(item -> item.get("date")).containsExactly("2026-10-10", "2026-10-12");

        Map meal = api.get("/api/meals/" + mealId).getBody();
        assertThat(meal).containsEntry("workingDays", 10).containsEntry("noServiceDays", 1)
                .containsEntry("serviceDays", 9).containsEntry("extraDays", 1);
        assertMoney(meal.get("dailyPrice"), "452.00");
        assertMoney(meal.get("extraDaysPrice"), "163.00");
        assertMoney(meal.get("totalPrice"), "4231.00");
        assertThat((List<Map>) meal.get("adjustments")).extracting(item -> item.get("date"))
                .containsExactly("2026-10-10", "2026-10-12");

        Map project = api.get("/api/projects/" + projectId).getBody();
        assertMoney(project.get("totalPrice"), "4231.00");
        Map projectMeal = ((List<Map>) project.get("meals")).get(0);
        assertThat(projectMeal).containsEntry("serviceDays", 9).doesNotContainKey("adjustments");
    }

    @Test
    @DisplayName("erros: 400 com cada codigo, 409 para data repetida, nada gravado")
    void validationErrors() {
        api.create(path(), adjustment("2026-10-12", "NO_SERVICE", null, null, null));

        assertError(adjustment("2026-10-20", "NO_SERVICE", null, null, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.date");
        assertError(adjustment("2026-10-04", "EXTRA_DAY", 6, 0, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.date");
        assertError(adjustment(null, "NO_SERVICE", null, null, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.date");
        assertError(adjustment("2026-10-12", "NO_SERVICE", null, null, null), HttpStatus.CONFLICT,
                "meal.adjustment.duplicate.date");
        assertError(adjustment("2026-10-07", "EXTRA_DAY", 6, 0, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.extra_day");
        assertError(adjustment("2026-10-10", "NO_SERVICE", null, null, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.no_service");
        assertError(adjustment("2026-10-10", "EXTRA_DAY", 0, 0, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.quantity");
        assertError(adjustment("2026-10-10", "EXTRA_DAY", 6, null, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.quantity");
        assertError(adjustment("2026-10-13", "NO_SERVICE", 5, null, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.quantity");
        assertError(adjustment("2026-10-13", null, null, null, null), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.type");
        assertError(adjustment("2026-10-13", "NO_SERVICE", null, null, "a".repeat(101)), HttpStatus.BAD_REQUEST,
                "meal.adjustment.invalid.note");

        assertThat(api.getList(path()).getBody()).hasSize(1);
    }

    @Test
    @DisplayName("remover: 204, total volta a contar o dia; ajuste inexistente ou refeicao inexistente: 404")
    void removeAdjustment() {
        String adjustmentId = api.create(path(), adjustment("2026-10-12", "NO_SERVICE", null, null, null));
        assertMoney(api.get("/api/meals/" + mealId).getBody().get("totalPrice"), "4068.00");

        ResponseEntity<Map> removed = api.delete(path() + "/" + adjustmentId);

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertMoney(api.get("/api/meals/" + mealId).getBody().get("totalPrice"), "4520.00");
        ResponseEntity<Map> again = api.delete(path() + "/" + adjustmentId);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(again.getBody()).containsEntry("errorCode", "meal.adjustment.not_found");
        ResponseEntity<Map> unknownMeal = api.get("/api/meals/00000000-0000-0000-0000-000000000000/adjustments");
        assertThat(unknownMeal.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownMeal.getBody()).containsEntry("errorCode", "meal.not_found");
    }

    @Test
    @DisplayName("mudar o termino da obra marca inPeriod falso e tira o ajuste do total, sem apagar")
    void projectEndDateChange() {
        api.create(path(), adjustment("2026-10-12", "NO_SERVICE", null, null, null));
        api.create(path(), adjustment("2026-10-10", "EXTRA_DAY", 6, 0, null));

        projectBody.put("endDate", "2026-10-09");
        assertThat(api.put("/api/projects/" + projectId, projectBody).getStatusCode()).isEqualTo(HttpStatus.OK);

        Map meal = api.get("/api/meals/" + mealId).getBody();
        assertThat((List<Map>) meal.get("adjustments")).hasSize(2)
                .allSatisfy(item -> assertThat(item).containsEntry("inPeriod", false));
        assertThat(meal).containsEntry("workingDays", 5).containsEntry("serviceDays", 5)
                .containsEntry("noServiceDays", 0).containsEntry("extraDays", 0);
        assertMoney(meal.get("totalPrice"), "2260.00");
    }

    @Test
    @DisplayName("PUT da refeicao mantem os ajustes; excluir a refeicao exclui os ajustes")
    void mealUpdateAndDelete() {
        api.create(path(), adjustment("2026-10-12", "NO_SERVICE", null, null, null));
        Map<String, Object> mealBody = api.mealBody(
                (String) ((Map) api.get("/api/meals/" + mealId).getBody().get("restaurant")).get("id"),
                "LUNCH_AND_DINNER");
        mealBody.put("lunchQuantity", 12);

        ResponseEntity<Map> updated = api.put("/api/meals/" + mealId, mealBody);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List) updated.getBody().get("adjustments")).hasSize(1);
        assertThat(api.getList(path()).getBody()).hasSize(1);

        assertThat(api.delete("/api/meals/" + mealId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(api.get(path()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("OPERADOR le os ajustes, mas nao inclui nem remove (403)")
    void operatorIsReadOnly() {
        String adjustmentId = api.create(path(), adjustment("2026-10-12", "NO_SERVICE", null, null, null));
        String email = AdminApi.unique("operador") + "@exemplo.test";
        api.create("/api/users", Map.of("name", "Operador Ajustes", "email", email, "password", "senha123",
                "isActive", true, "roles", List.of("OPERADOR")));
        String token = (String) rest.postForEntity("/api/auth/login", Map.of("email", email, "password", "senha123"),
                Map.class).getBody().get("accessToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        assertThat(rest.exchange(path(), HttpMethod.GET, new HttpEntity<>(headers), List.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(rest.exchange(path(), HttpMethod.POST,
                new HttpEntity<>(adjustment("2026-10-13", "NO_SERVICE", null, null, null), headers), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rest.exchange(path() + "/" + adjustmentId, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.getList(path()).getBody()).hasSize(1);
    }

    private void assertError(Map<String, Object> body, HttpStatus status, String errorCode) {
        ResponseEntity<Map> response = api.post(path(), body);
        assertThat(response.getStatusCode()).as(body + " -> " + response.getBody()).isEqualTo(status);
        assertThat(response.getBody()).containsEntry("errorCode", errorCode);
    }

    private static void assertMoney(Object actual, String expected) {
        assertThat(actual).isNotNull();
        assertThat(new BigDecimal(actual.toString())).isEqualByComparingTo(expected);
    }
}
