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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Contrato de refeicao por obra (change refeicoes-por-obra-e-email-opcional). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class MealPlanIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    /** Obra de segunda 2026-10-05 a sexta 2026-10-16: 10 dias uteis. */
    private Map<String, Object> twoWeekProjectBody() {
        Map<String, Object> body = api.projectBody(api.createCostCenter(), api.createClient());
        body.put("startDate", "2026-10-05");
        body.put("endDate", "2026-10-16");
        return body;
    }

    @Test
    @DisplayName("LUNCH_AND_DINNER deve trazer campos por refeicao, custo por dia, dias uteis e total")
    void lunchAndDinnerWithEstimatedTotal() {
        String projectId = api.create("/api/projects", twoWeekProjectBody());
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(projectId));

        ResponseEntity<Map> created = api.post("/api/meals", api.mealBody(restaurantId, "LUNCH_AND_DINNER"));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map meal = api.get("/api/meals/" + created.getBody().get("id")).getBody();
        assertThat(meal).containsEntry("mealType", "LUNCH_AND_DINNER")
                .containsEntry("lunchQuantity", 10)
                .containsEntry("dinnerQuantity", 8)
                .containsEntry("workingDays", 10)
                .containsKeys("restaurant", "project", "isBilled", "createdAt", "updatedAt")
                .doesNotContainKeys("date", "quantity", "price");
        assertMoney(meal.get("lunchPrice"), "25.00");
        assertMoney(meal.get("dinnerPrice"), "22.00");
        assertMoney(meal.get("dailyPrice"), "426.00");
        assertMoney(meal.get("totalPrice"), "4260.00");

        Map project = api.get("/api/projects/" + projectId).getBody();
        assertThat(project).doesNotContainKeys("lunch", "dinner");
        List<Map> meals = (List<Map>) project.get("meals");
        assertThat(meals).singleElement().satisfies(summary -> {
            assertThat(summary).containsEntry("id", meal.get("id")).doesNotContainKey("project");
            assertMoney(summary.get("totalPrice"), "4260.00");
        });
        assertMoney(project.get("totalPrice"), "4260.00");
    }

    @Test
    @DisplayName("campos da refeicao nao servida sao ignorados e devolvidos null")
    void fieldsOfMealNotServedAreNull() {
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(api.createProject()));

        Map meal = api.post("/api/meals", api.mealBody(restaurantId, "DINNER")).getBody();

        assertThat(meal.get("lunchQuantity")).isNull();
        assertThat(meal.get("lunchPrice")).isNull();
        assertThat(meal).containsEntry("dinnerQuantity", 8);
        assertMoney(meal.get("dailyPrice"), "176.00");
    }

    @Test
    @DisplayName("obra sem termino deve devolver workingDays e totalPrice null")
    void projectWithoutEndDate() {
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(api.createProject()));

        Map meal = api.post("/api/meals", api.mealBody(restaurantId, "LUNCH")).getBody();

        assertMoney(meal.get("dailyPrice"), "250.00");
        assertThat(meal).containsKeys("workingDays", "totalPrice");
        assertThat(meal.get("workingDays")).isNull();
        assertThat(meal.get("totalPrice")).isNull();
    }

    @Test
    @DisplayName("alterar o termino da obra recalcula dias uteis e total da refeicao")
    void projectEndDateChangeRecalculatesMeal() {
        Map<String, Object> projectBody = twoWeekProjectBody();
        String projectId = api.create("/api/projects", projectBody);
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(projectId));
        String mealId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH_AND_DINNER"));

        projectBody.put("endDate", "2026-10-09");
        ResponseEntity<Map> updated = api.put("/api/projects/" + projectId, projectBody);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map summary = ((List<Map>) updated.getBody().get("meals")).get(0);
        assertThat(summary).containsEntry("id", mealId).containsEntry("workingDays", 5);
        assertMoney(summary.get("totalPrice"), "2130.00");
        Map meal = api.get("/api/meals/" + mealId).getBody();
        assertThat(meal).containsEntry("workingDays", 5);
        assertMoney(meal.get("totalPrice"), "2130.00");
    }

    @Test
    @DisplayName("LUNCH_AND_DINNER ocupa almoco e jantar: novo LUNCH ou DINNER responde 409 meal.duplicate.type")
    void coverageOverlapConflicts() {
        String projectId = api.createProject();
        String first = api.create("/api/restaurants", api.restaurantBody(projectId));
        String second = api.create("/api/restaurants", api.restaurantBody(projectId));
        api.create("/api/meals", api.mealBody(first, "LUNCH_AND_DINNER"));

        for (String type : List.of("LUNCH", "DINNER", "LUNCH_AND_DINNER")) {
            ResponseEntity<Map> response = api.post("/api/meals", api.mealBody(second, type));
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).containsEntry("errorCode", "meal.duplicate.type");
        }
        assertThat((List) api.get("/api/projects/" + projectId).getBody().get("meals")).hasSize(1);
    }

    @Test
    @DisplayName("refeicao LUNCH pode virar LUNCH_AND_DINNER se a obra nao tem jantar; com jantar, 409")
    void editingKeepsOwnCoverage() {
        String projectId = api.createProject();
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(projectId));
        Map<String, Object> body = api.mealBody(restaurantId, "LUNCH");
        String lunchId = api.create("/api/meals", body);

        body.put("mealType", "LUNCH_AND_DINNER");
        assertThat(api.put("/api/meals/" + lunchId, body).getStatusCode()).isEqualTo(HttpStatus.OK);

        body.put("mealType", "LUNCH");
        api.put("/api/meals/" + lunchId, body);
        api.create("/api/meals", api.mealBody(restaurantId, "DINNER"));
        body.put("mealType", "LUNCH_AND_DINNER");
        ResponseEntity<Map> blocked = api.put("/api/meals/" + lunchId, body);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(blocked.getBody()).containsEntry("errorCode", "meal.duplicate.type");
    }

    @Test
    @DisplayName("quantidade e preco invalidos respondem 400 com o codigo do campo")
    void invalidFieldsPerServedMeal() {
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(api.createProject()));

        assertInvalid(restaurantId, "LUNCH", "lunchQuantity", 0, "meal.invalid.lunch_quantity");
        assertInvalid(restaurantId, "LUNCH", "lunchQuantity", 10001, "meal.invalid.lunch_quantity");
        assertInvalid(restaurantId, "LUNCH_AND_DINNER", "lunchPrice", 0, "meal.invalid.lunch_price");
        assertInvalid(restaurantId, "DINNER", "dinnerQuantity", null, "meal.invalid.dinner_quantity");
        assertInvalid(restaurantId, "LUNCH_AND_DINNER", "dinnerPrice", null, "meal.invalid.dinner_price");
        assertInvalid(restaurantId, null, "mealType", null, "meal.invalid.type");
    }

    // --- e-mail opcional ---

    @Test
    @DisplayName("dois clientes sem e-mail sao aceitos; e-mail invalido 400; repetido 409")
    void clientEmailIsOptional() {
        Map<String, Object> blank = api.clientBody();
        blank.put("email", "  ");
        Map<String, Object> missing = api.clientBody();
        missing.remove("email");

        ResponseEntity<Map> first = api.post("/api/clients", blank);
        ResponseEntity<Map> second = api.post("/api/clients", missing);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody()).containsEntry("email", null);
        assertThat(second.getBody()).containsEntry("email", null);

        Map<String, Object> invalid = api.clientBody();
        invalid.put("email", "cliente@");
        assertThat(api.post("/api/clients", invalid).getBody()).containsEntry("errorCode", "client.invalid.email");

        Map<String, Object> original = api.clientBody();
        api.create("/api/clients", original);
        Map<String, Object> duplicate = api.clientBody();
        duplicate.put("email", original.get("email"));
        ResponseEntity<Map> conflict = api.post("/api/clients", duplicate);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).containsEntry("errorCode", "client.duplicate.email");
    }

    @Test
    @DisplayName("dois colaboradores sem e-mail sao aceitos; e-mail invalido 400; repetido 409")
    void employeeEmailIsOptional() {
        String costCenterId = api.createCostCenter();
        Map<String, Object> blank = api.employeeBody(costCenterId);
        blank.put("email", "");
        Map<String, Object> missing = api.employeeBody(costCenterId);
        missing.put("email", null);

        ResponseEntity<Map> first = api.post("/api/employees", blank);
        ResponseEntity<Map> second = api.post("/api/employees", missing);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody()).containsEntry("email", null);
        assertThat(api.get("/api/employees/" + second.getBody().get("id")).getBody()).containsEntry("email", null);

        Map<String, Object> invalid = api.employeeBody(costCenterId);
        invalid.put("email", "montador@");
        assertThat(api.post("/api/employees", invalid).getBody())
                .containsEntry("errorCode", "employee.invalid.email");

        Map<String, Object> original = api.employeeBody(costCenterId);
        api.create("/api/employees", original);
        Map<String, Object> duplicate = api.employeeBody(costCenterId);
        duplicate.put("email", original.get("email"));
        ResponseEntity<Map> conflict = api.post("/api/employees", duplicate);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).containsEntry("errorCode", "employee.duplicate.email");
    }

    private void assertInvalid(String restaurantId, String type, String field, Object value, String errorCode) {
        Map<String, Object> body = api.mealBody(restaurantId, type);
        body.put(field, value);
        ResponseEntity<Map> response = api.post("/api/meals", body);
        assertThat(response.getStatusCode()).as(field + "=" + value).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("errorCode", errorCode);
    }

    private static void assertMoney(Object actual, String expected) {
        assertThat(actual).isNotNull();
        assertThat(new BigDecimal(actual.toString())).isEqualByComparingTo(expected);
    }
}
