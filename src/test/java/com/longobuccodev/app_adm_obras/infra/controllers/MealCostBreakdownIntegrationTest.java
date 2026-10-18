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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Taxa de entrega, refrigerante e projecao por periodo (changes restaurante-entrega-refrigerante-periodos e
 * refrigerante-no-restaurante: o refrigerante e do restaurante).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class MealCostBreakdownIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    /** Obra de segunda 2026-10-05 a sexta 2026-10-16: 10 dias uteis. */
    private String twoWeekProject() {
        Map<String, Object> body = api.projectBody(api.createCostCenter(), api.createClient());
        body.put("startDate", "2026-10-05");
        body.put("endDate", "2026-10-16");
        return api.create("/api/projects", body);
    }

    private Map<String, Object> restaurantWithFee(String projectId, Object fee) {
        return restaurantWith(projectId, fee, null);
    }

    private Map<String, Object> restaurantWith(String projectId, Object fee, Object sodaPrice) {
        Map<String, Object> body = api.restaurantBody(projectId);
        body.put("lunchDeliveryFee", fee);
        body.put("dinnerDeliveryFee", fee);
        body.put("sodaPrice", sodaPrice);
        return body;
    }

    @Test
    @DisplayName("refeicao com taxa e refrigerante traz parcelas, projecoes e total da obra com entrega e refrigerante")
    void mealWithDeliveryAndSoda() {
        String projectId = twoWeekProject();
        String restaurantId = api.create("/api/restaurants", restaurantWith(projectId, 8, 5));

        ResponseEntity<Map> created = api.post("/api/meals", api.mealBody(restaurantId, "LUNCH_AND_DINNER"));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map meal = api.get("/api/meals/" + created.getBody().get("id")).getBody();
        assertMoney(((Map) meal.get("restaurant")).get("lunchDeliveryFee"), "8.00");
        assertMoney(((Map) meal.get("restaurant")).get("sodaPrice"), "5.00");
        assertMoney(meal.get("sodaPrice"), "5.00");
        assertThat(meal).containsEntry("deliveriesPerDay", 2).containsEntry("workingDays", 10);
        assertMoney(meal.get("mealsDailyPrice"), "426.00");
        assertMoney(meal.get("deliveryDailyPrice"), "16.00");
        assertMoney(meal.get("sodaDailyPrice"), "10.00");
        assertMoney(meal.get("dailyPrice"), "452.00");
        assertMoney(meal.get("totalPrice"), "4520.00");

        List<Map> projections = (List<Map>) meal.get("projections");
        assertThat(projections).extracting(item -> item.get("period"))
                .containsExactly("DAY", "WEEK", "FORTNIGHT", "MONTH");
        assertThat(projections.get(1)).containsEntry("workingDays", 5).containsEntry("lunchMeals", 50)
                .containsEntry("dinnerMeals", 40).containsEntry("deliveries", 10).containsEntry("sodas", 10);
        assertMoney(projections.get(0).get("price"), "452.00");
        assertMoney(projections.get(1).get("price"), "2260.00");
        assertMoney(projections.get(2).get("price"), "4520.00");
        assertThat(projections.get(3)).containsEntry("workingDays", 22).containsEntry("lunchMeals", 220)
                .containsEntry("dinnerMeals", 176).containsEntry("deliveries", 44).containsEntry("sodas", 44);
        assertMoney(projections.get(3).get("price"), "9944.00");

        Map project = api.get("/api/projects/" + projectId).getBody();
        assertMoney(project.get("totalPrice"), "4520.00");
        assertMoney(((Map) ((List) project.get("restaurants")).get(0)).get("lunchDeliveryFee"), "8.00");
        assertMoney(((Map) ((List) project.get("restaurants")).get(0)).get("sodaPrice"), "5.00");
    }

    @Test
    @DisplayName("sem taxa e sem refrigerante os valores ficam como antes")
    void withoutFeeAndSodaKeepsValues() {
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(twoWeekProject()));

        Map meal = api.post("/api/meals", api.mealBody(restaurantId, "LUNCH_AND_DINNER")).getBody();

        assertThat(meal.get("sodaPrice")).isNull();
        assertThat(((Map) meal.get("restaurant")).get("lunchDeliveryFee")).isNull();
        assertMoney(meal.get("deliveryDailyPrice"), "0");
        assertMoney(meal.get("sodaDailyPrice"), "0");
        assertMoney(meal.get("dailyPrice"), "426.00");
        assertMoney(meal.get("totalPrice"), "4260.00");
        assertThat(((List<Map>) meal.get("projections")).get(1)).containsEntry("sodas", 0);
    }

    @Test
    @DisplayName("restaurante com taxa e refeicoes: detalhe, listagem e listagem por obra trazem meals")
    void restaurantResponsesCarryMeals() {
        String projectId = twoWeekProject();
        String withMeal = api.create("/api/restaurants", restaurantWith(projectId, 8, 5));
        String withoutMeal = api.create("/api/restaurants", api.restaurantBody(projectId));
        String lunch = api.create("/api/meals", api.mealBody(withMeal, "LUNCH"));
        String dinner = api.create("/api/meals", api.mealBody(withMeal, "DINNER"));

        Map detail = api.get("/api/restaurants/" + withMeal).getBody();
        assertMoney(detail.get("lunchDeliveryFee"), "8.00");
        assertMoney(detail.get("sodaPrice"), "5.00");
        List<Map> meals = (List<Map>) detail.get("meals");
        assertThat(meals).extracting(item -> item.get("id")).containsExactly(lunch, dinner);
        assertThat(meals.get(0)).doesNotContainKey("project").containsKey("projections");
        assertMoney(meals.get(0).get("dailyPrice"), "263.00");
        assertMoney(meals.get(1).get("dailyPrice"), "189.00");

        List<Map> byProject = api.getList("/api/restaurants/by-project/" + projectId).getBody();
        assertThat(byProject).extracting(item -> ((List) item.get("meals")).size())
                .containsExactlyInAnyOrder(2, 0);

        List<Map> page = allRestaurants();
        Map listedWithMeal = page.stream().filter(item -> withMeal.equals(item.get("id"))).findFirst().orElseThrow();
        Map listedWithoutMeal = page.stream().filter(item -> withoutMeal.equals(item.get("id"))).findFirst()
                .orElseThrow();
        assertThat((List<Map>) listedWithMeal.get("meals")).extracting(item -> item.get("id"))
                .containsExactly(lunch, dinner);
        assertThat((List) listedWithoutMeal.get("meals")).isEmpty();
        assertMoney(listedWithMeal.get("sodaPrice"), "5.00");
        assertThat(listedWithoutMeal.get("sodaPrice")).isNull();

        Map createdResponse = api.post("/api/restaurants", api.restaurantBody(projectId)).getBody();
        assertThat((List) createdResponse.get("meals")).isEmpty();
    }

    @Test
    @DisplayName("alterar a taxa do restaurante muda o custo da refeicao na leitura seguinte")
    void feeChangeReflectsOnMeal() {
        String projectId = twoWeekProject();
        Map<String, Object> restaurant = api.restaurantBody(projectId);
        String restaurantId = api.create("/api/restaurants", restaurant);
        String mealId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));

        restaurant.put("lunchDeliveryFee", 8);
        restaurant.put("dinnerDeliveryFee", 8);
        ResponseEntity<Map> updated = api.put("/api/restaurants/" + restaurantId, restaurant);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertMoney(((Map) ((List) updated.getBody().get("meals")).get(0)).get("deliveryDailyPrice"), "8.00");
        Map meal = api.get("/api/meals/" + mealId).getBody();
        assertMoney(meal.get("deliveryDailyPrice"), "8.00");
        assertMoney(meal.get("dailyPrice"), "258.00");
    }

    @Test
    @DisplayName("alterar o refrigerante do restaurante muda todas as refeicoes dele e o total da obra")
    void sodaPriceChangeReflectsOnEveryMeal() {
        String projectId = twoWeekProject();
        Map<String, Object> restaurant = restaurantWith(projectId, 8, 5);
        String restaurantId = api.create("/api/restaurants", restaurant);
        String lunchId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        String dinnerId = api.create("/api/meals", api.mealBody(restaurantId, "DINNER"));
        // LUNCH 10 x 25 + 8 + 5 = 263; DINNER 8 x 22 + 8 + 5 = 189; 10 dias uteis
        assertMoney(api.get("/api/projects/" + projectId).getBody().get("totalPrice"), "4520.00");

        restaurant.put("sodaPrice", 6);
        ResponseEntity<Map> updated = api.put("/api/restaurants/" + restaurantId, restaurant);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertMoney(updated.getBody().get("sodaPrice"), "6.00");
        for (String mealId : List.of(lunchId, dinnerId)) {
            Map meal = api.get("/api/meals/" + mealId).getBody();
            assertMoney(meal.get("sodaPrice"), "6.00");
            assertMoney(meal.get("sodaDailyPrice"), "6.00");
        }
        assertMoney(api.get("/api/meals/" + lunchId).getBody().get("dailyPrice"), "264.00");
        assertMoney(api.get("/api/projects/" + projectId).getBody().get("totalPrice"), "4540.00");

        restaurant.put("sodaPrice", null);
        api.put("/api/restaurants/" + restaurantId, restaurant);

        Map lunch = api.get("/api/meals/" + lunchId).getBody();
        assertThat(lunch.get("sodaPrice")).isNull();
        assertMoney(lunch.get("sodaDailyPrice"), "0");
        assertThat(((List<Map>) lunch.get("projections")).get(3)).containsEntry("sodas", 0);
    }

    @Test
    @DisplayName("sodaPrice enviado na refeicao e ignorado: vale o do restaurante")
    void sodaPriceOnMealIsIgnored() {
        String projectId = twoWeekProject();
        String withoutSoda = api.create("/api/restaurants", api.restaurantBody(projectId));
        Map<String, Object> body = api.mealBody(withoutSoda, "LUNCH");
        body.put("sodaPrice", 9);

        ResponseEntity<Map> created = api.post("/api/meals", body);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("sodaPrice")).isNull();
        assertMoney(created.getBody().get("sodaDailyPrice"), "0");

        body.put("sodaPrice", -1);
        ResponseEntity<Map> updated = api.put("/api/meals/" + created.getBody().get("id"), body);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("sodaPrice")).isNull();
    }

    @Test
    @DisplayName("taxa e refrigerante zero ou negativos respondem 400 com o codigo do campo")
    void invalidFeeAndSoda() {
        String projectId = api.createProject();
        for (Object value : new Object[]{0, -1}) {
            ResponseEntity<Map> restaurant = api.post("/api/restaurants", restaurantWithFee(projectId, value));
            assertThat(restaurant.getStatusCode()).as("deliveryFee=" + value).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(restaurant.getBody()).containsEntry("errorCode", "restaurant.invalid.delivery_fee");
        }

        Map<String, Object> restaurantBody = api.restaurantBody(projectId);
        String restaurantId = api.create("/api/restaurants", restaurantBody);
        restaurantBody.put("dinnerDeliveryFee", 0);
        assertThat(api.put("/api/restaurants/" + restaurantId, restaurantBody).getBody())
                .containsEntry("errorCode", "restaurant.invalid.delivery_fee");

        for (Object value : new Object[]{0, -1}) {
            ResponseEntity<Map> created = api.post("/api/restaurants", restaurantWith(projectId, null, value));
            assertThat(created.getStatusCode()).as("sodaPrice=" + value).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(created.getBody()).containsEntry("errorCode", "restaurant.invalid.soda_price");
        }
        restaurantBody.put("dinnerDeliveryFee", null);
        restaurantBody.put("sodaPrice", -1);
        assertThat(api.put("/api/restaurants/" + restaurantId, restaurantBody).getBody())
                .containsEntry("errorCode", "restaurant.invalid.soda_price");
        Map stored = api.get("/api/restaurants/" + restaurantId).getBody();
        assertThat(stored.get("sodaPrice")).isNull();
        assertThat(stored.get("lunchDeliveryFee")).isNull();
        assertThat(stored.get("dinnerDeliveryFee")).isNull();
    }

    /** Todas as paginas de GET /api/restaurants (o banco H2 e compartilhado entre as classes de teste). */
    private List<Map> allRestaurants() {
        List<Map> all = new ArrayList<>();
        int totalPages;
        int page = 0;
        do {
            Map body = api.get("/api/restaurants?size=100&page=" + page).getBody();
            all.addAll((List<Map>) body.get("content"));
            totalPages = ((Number) body.get("totalPages")).intValue();
            page++;
        } while (page < totalPages);
        return all;
    }

    private static void assertMoney(Object actual, String expected) {
        assertThat(actual).isNotNull();
        assertThat(new BigDecimal(actual.toString())).isEqualByComparingTo(expected);
    }
}
