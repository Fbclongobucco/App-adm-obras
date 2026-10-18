package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.support.AdminApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Taxa de entrega por refeicao e gravacao do dia por data (change refeicoes-editaveis-por-dia). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class MealDayChangesIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    /** Obra de 2026-10-05 a 2026-10-16 e restaurante com entrega 8.00 (almoco), 10.00 (jantar), refrigerante 5.00. */
    private String restaurant() {
        Map<String, Object> project = api.projectBody(api.createCostCenter(), api.createClient());
        project.put("startDate", "2026-10-05");
        project.put("endDate", "2026-10-16");
        Map<String, Object> restaurant = api.restaurantBody(api.create("/api/projects", project));
        restaurant.put("lunchDeliveryFee", 8);
        restaurant.put("dinnerDeliveryFee", 10);
        restaurant.put("sodaPrice", 5);
        return api.create("/api/restaurants", restaurant);
    }

    private static Map<String, Object> day(String type, Integer lunch, Integer dinner, Integer lunchDeliveries,
                                           Integer dinnerDeliveries, Integer sodas) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("lunchQuantity", lunch);
        body.put("dinnerQuantity", dinner);
        body.put("lunchDeliveries", lunchDeliveries);
        body.put("dinnerDeliveries", dinnerDeliveries);
        body.put("sodas", sodas);
        return body;
    }

    private static Map<String, Object> restaurantDay(String type, List<Map<String, Object>> meals) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("note", "Teste");
        body.put("meals", meals);
        return body;
    }

    private static Map<String, Object> mealDay(String mealId, Integer lunch, Integer dinner, Integer lunchDeliveries,
                                               Integer dinnerDeliveries) {
        Map<String, Object> body = day(null, lunch, dinner, lunchDeliveries, dinnerDeliveries, null);
        body.remove("type");
        body.put("mealId", mealId);
        return body;
    }

    private static void assertMoney(Object actual, String expected) {
        assertThat(new BigDecimal(actual.toString())).isEqualByComparingTo(expected);
    }

    private List<Map> adjustments(String mealId) {
        return (List<Map>) api.get("/api/meals/" + mealId).getBody().get("adjustments");
    }

    @Test
    @DisplayName("restaurante com duas taxas: contrato e custo por dia de 454,00")
    void twoFees() {
        String restaurantId = restaurant();
        String mealId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH_AND_DINNER"));

        Map restaurant = api.get("/api/restaurants/" + restaurantId).getBody();
        assertMoney(restaurant.get("lunchDeliveryFee"), "8.00");
        assertMoney(restaurant.get("dinnerDeliveryFee"), "10.00");
        assertThat(restaurant).doesNotContainKey("deliveryFee");
        Map meal = api.get("/api/meals/" + mealId).getBody();
        assertMoney(meal.get("deliveryDailyPrice"), "18.00");
        assertMoney(meal.get("dailyPrice"), "454.00");
        assertMoney(meal.get("totalPrice"), "4540.00");
    }

    @Test
    @DisplayName("PUT do dia na refeicao: cria 489,00, substitui com o mesmo id, total e semana")
    void putMealDay() {
        String mealId = api.create("/api/meals", api.mealBody(restaurant(), "LUNCH_AND_DINNER"));
        String path = "/api/meals/" + mealId + "/adjustments/2026-10-14";

        ResponseEntity<Map> created = api.put(path, day("CHANGED", 10, 9, 2, 1, null));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).containsEntry("type", "CHANGED").containsEntry("lunchDeliveries", 2)
                .containsEntry("dinnerDeliveries", 1).containsEntry("deliveries", 3).containsEntry("sodas", 3);
        assertMoney(created.getBody().get("price"), "489.00");
        Map meal = api.get("/api/meals/" + mealId).getBody();
        assertThat(meal).containsEntry("changedDays", 1);
        assertMoney(meal.get("totalPrice"), "4575.00");
        Map closing = api.get("/api/meals/" + mealId + "/weekly-closing?week=2026-10-12").getBody();
        Map wednesday = ((List<Map>) closing.get("days")).get(2);
        assertThat(wednesday).containsEntry("status", "CHANGED").containsEntry("lunchDeliveries", 2)
                .containsEntry("deliveries", 3);
        assertMoney(((Map) closing.get("totals")).get("price"), "2305.00");

        ResponseEntity<Map> replaced = api.put(path, day("CHANGED", 10, 9, 2, 1, 2));
        assertThat(replaced.getBody()).containsEntry("id", created.getBody().get("id"));
        assertMoney(replaced.getBody().get("price"), "484.00");
        assertThat(adjustments(mealId)).hasSize(1);
    }

    @Test
    @DisplayName("PUT do dia na refeicao: erros com os codigos da spec e nada gravado")
    void putMealDayErrors() {
        String mealId = api.create("/api/meals", api.mealBody(restaurant(), "LUNCH_AND_DINNER"));

        assertThat(api.put("/api/meals/" + mealId + "/adjustments/2026-10-10", day("CHANGED", 6, 0, null, null,
                null)).getBody()).containsEntry("errorCode", "meal.adjustment.invalid.changed_day");
        ResponseEntity<Map> withoutMeal = api.put("/api/meals/" + mealId + "/adjustments/2026-10-14",
                day("CHANGED", 10, 0, null, 1, null));
        assertThat(withoutMeal.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(withoutMeal.getBody()).containsEntry("errorCode", "meal.adjustment.invalid.delivery_without_meal");
        assertThat(adjustments(mealId)).isEmpty();
    }

    @Test
    @DisplayName("PUT do dia no restaurante: jantar zerado fica sem fornecimento, almoco no padrao sem ajuste")
    void putRestaurantDay() {
        String restaurantId = restaurant();
        String lunchId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        String dinnerId = api.create("/api/meals", api.mealBody(restaurantId, "DINNER"));

        ResponseEntity<Map> response = api.put("/api/restaurants/" + restaurantId + "/adjustments/2026-10-14",
                restaurantDay("CHANGED", List.of(mealDay(lunchId, 10, null, null, null),
                        mealDay(dinnerId, null, 0, null, null))));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adjustments(lunchId)).isEmpty();
        assertThat(adjustments(dinnerId)).singleElement().satisfies(item -> assertThat(item)
                .containsEntry("type", "NO_SERVICE").containsEntry("note", "Teste"));
        Map wednesday = ((List<Map>) api.get("/api/restaurants/" + restaurantId + "/weekly-closing?week=2026-10-12")
                .getBody().get("days")).get(2);
        assertThat(wednesday).containsEntry("lunchMeals", 10).containsEntry("dinnerMeals", 0)
                .containsEntry("deliveries", 1).containsEntry("partialNoService", true);
        assertThat((List<Map>) wednesday.get("meals")).extracting(meal -> meal.get("mealId"))
                .containsExactlyInAnyOrder(lunchId, dinnerId);
    }

    @Test
    @DisplayName("PUT do dia no restaurante: erro numa refeicao nao grava nada; dia extra regravado")
    void putRestaurantDayAtomicAndExtraDay() {
        String restaurantId = restaurant();
        String lunchId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        String dinnerId = api.create("/api/meals", api.mealBody(restaurantId, "DINNER"));
        String base = "/api/restaurants/" + restaurantId + "/adjustments/";

        ResponseEntity<Map> error = api.put(base + "2026-10-14", restaurantDay("CHANGED",
                List.of(mealDay(lunchId, 9, null, null, null), mealDay(dinnerId, null, 0, null, 1))));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.getBody()).containsEntry("errorCode", "meal.adjustment.invalid.delivery_without_meal");
        assertThat(adjustments(lunchId)).isEmpty();
        assertThat(adjustments(dinnerId)).isEmpty();

        api.put(base + "2026-10-10", restaurantDay("EXTRA_DAY",
                List.of(mealDay(lunchId, 6, null, null, null), mealDay(dinnerId, null, 4, null, null))));
        assertThat(adjustments(dinnerId)).singleElement().satisfies(item -> assertMoney(item.get("price"), "103.00"));
        String lunchAdjustment = (String) adjustments(lunchId).get(0).get("id");

        assertThat(api.put(base + "2026-10-10", restaurantDay("EXTRA_DAY",
                List.of(mealDay(lunchId, 7, null, null, null), mealDay(dinnerId, null, 0, null, null))))
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adjustments(dinnerId)).isEmpty();
        assertThat(adjustments(lunchId)).singleElement().satisfies(item -> {
            assertThat(item).containsEntry("id", lunchAdjustment).containsEntry("lunchQuantity", 7);
            assertMoney(item.get("price"), "188.00");
        });

        assertThat(api.put(base + "2026-10-11", restaurantDay("EXTRA_DAY",
                List.of(mealDay(lunchId, 0, null, null, null)))).getBody())
                .containsEntry("errorCode", "meal.adjustment.invalid.quantity");
    }
}
