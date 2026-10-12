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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class RestaurantIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    // --- restaurante ---

    @Test
    @DisplayName("restaurante criado para obra existente deve trazer a obra vinculada")
    void createForExistingProject() {
        String projectId = api.createProject();

        ResponseEntity<Map> response = api.post("/api/restaurants", api.restaurantBody(projectId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Map) response.getBody().get("project")).get("id")).isEqualTo(projectId);
        assertThat(response.getBody()).containsKeys("createdAt", "updatedAt", "address");
    }

    @Test
    @DisplayName("restaurante sem obra deve responder 400 restaurant.invalid.project e nada e gravado")
    void createWithoutProject() {
        Map<String, Object> body = api.restaurantBody(null);
        long before = total();

        ResponseEntity<Map> response = api.post("/api/restaurants", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("errorCode", "restaurant.invalid.project");
        assertThat(total()).isEqualTo(before);
    }

    @Test
    @DisplayName("restaurante com obra inexistente deve responder 404 project.not_found")
    void createWithUnknownProject() {
        ResponseEntity<Map> response = api.post("/api/restaurants",
                api.restaurantBody(UUID.randomUUID().toString()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("errorCode", "project.not_found");
    }

    @Test
    @DisplayName("listagem por obra deve trazer apenas os restaurantes daquela obra")
    void listByProject() {
        String projectA = api.createProject();
        String projectB = api.createProject();
        String restaurantA = api.create("/api/restaurants", api.restaurantBody(projectA));
        api.create("/api/restaurants", api.restaurantBody(projectB));

        List<Map> listed = api.getList("/api/restaurants/by-project/" + projectA).getBody();

        assertThat(listed).extracting(item -> item.get("id")).containsExactly(restaurantA);
    }

    @Test
    @DisplayName("restaurante sem refeicoes pode mudar de obra; com refeicoes responde 409 restaurant.in_use")
    void moveBetweenProjects() {
        String projectA = api.createProject();
        String projectB = api.createProject();
        Map<String, Object> body = api.restaurantBody(projectA);
        String restaurantId = api.create("/api/restaurants", body);

        body.put("projectId", projectB);
        assertThat(api.put("/api/restaurants/" + restaurantId, body).getStatusCode()).isEqualTo(HttpStatus.OK);

        api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        body.put("projectId", projectA);
        ResponseEntity<Map> blocked = api.put("/api/restaurants/" + restaurantId, body);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(blocked.getBody()).containsEntry("errorCode", "restaurant.in_use");
        assertThat(((Map) api.get("/api/restaurants/" + restaurantId).getBody().get("project")).get("id"))
                .isEqualTo(projectB);
    }

    @Test
    @DisplayName("restaurante com refeicoes nao pode ser excluido; sem refeicoes, sim")
    void deleteRestaurant() {
        String projectId = api.createProject();
        String withMeal = api.create("/api/restaurants", api.restaurantBody(projectId));
        api.create("/api/meals", api.mealBody(withMeal, "DINNER"));
        String withoutMeal = api.create("/api/restaurants", api.restaurantBody(projectId));

        ResponseEntity<Map> blocked = api.delete("/api/restaurants/" + withMeal);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(blocked.getBody()).containsEntry("errorCode", "restaurant.in_use");
        assertThat(api.delete("/api/restaurants/" + withoutMeal).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // --- refeicao ---

    @Test
    @DisplayName("refeicao deve herdar a obra do restaurante")
    void mealInheritsProject() {
        String projectId = api.createProject();
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(projectId));

        ResponseEntity<Map> meal = api.post("/api/meals", api.mealBody(restaurantId, "LUNCH"));

        assertThat(meal.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Map) meal.getBody().get("restaurant")).get("id")).isEqualTo(restaurantId);
        assertThat(((Map) meal.getBody().get("project")).get("id")).isEqualTo(projectId);
        List<Map> meals = (List<Map>) api.get("/api/projects/" + projectId).getBody().get("meals");
        assertThat(meals).extracting(item -> item.get("id")).containsExactly(meal.getBody().get("id"));
    }

    @Test
    @DisplayName("refeicao sem restaurante responde 400; com restaurante inexistente responde 404")
    void mealRestaurantIsRequired() {
        Map<String, Object> body = api.mealBody(null, "LUNCH");
        ResponseEntity<Map> missing = api.post("/api/meals", body);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missing.getBody()).containsEntry("errorCode", "meal.invalid.restaurant");

        body.put("restaurantId", UUID.randomUUID().toString());
        ResponseEntity<Map> unknown = api.post("/api/meals", body);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknown.getBody()).containsEntry("errorCode", "restaurant.not_found");
    }

    @Test
    @DisplayName("segundo almoco por outro restaurante da mesma obra deve responder 409 meal.duplicate.type")
    void secondLunchThroughAnotherRestaurant() {
        String projectId = api.createProject();
        String first = api.create("/api/restaurants", api.restaurantBody(projectId));
        String second = api.create("/api/restaurants", api.restaurantBody(projectId));
        api.create("/api/meals", api.mealBody(first, "LUNCH"));

        ResponseEntity<Map> response = api.post("/api/meals", api.mealBody(second, "LUNCH"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("errorCode", "meal.duplicate.type");
        assertThat(api.post("/api/meals", api.mealBody(second, "DINNER")).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    // --- obra ---

    @Test
    @DisplayName("PUT de obra nao move refeicoes: lunchId de outra obra no corpo e ignorado")
    void projectUpdateDoesNotMoveMeals() {
        String projectA = api.createProject();
        Map<String, Object> projectBBody = api.projectBody(api.createCostCenter(), api.createClient());
        String projectB = api.create("/api/projects", projectBBody);
        String restaurantA = api.create("/api/restaurants", api.restaurantBody(projectA));
        String mealA = api.create("/api/meals", api.mealBody(restaurantA, "LUNCH"));

        projectBBody.put("lunchId", mealA);
        ResponseEntity<Map> response = api.put("/api/projects/" + projectB, projectBBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List) response.getBody().get("meals")).isEmpty();
        assertThat(response.getBody()).doesNotContainKeys("lunch", "dinner");
        assertThat(((Map) api.get("/api/meals/" + mealA).getBody().get("project")).get("id")).isEqualTo(projectA);
    }

    @Test
    @DisplayName("PUT de obra deve manter almoco e janta vinculados pelo restaurante")
    void projectUpdateWithoutMealIdsKeepsMeals() {
        Map<String, Object> projectBody = api.projectBody(api.createCostCenter(), api.createClient());
        String projectId = api.create("/api/projects", projectBody);
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(projectId));
        String lunch = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        String dinner = api.create("/api/meals", api.mealBody(restaurantId, "DINNER"));

        projectBody.put("description", "Alterada sem refeicoes no corpo");
        assertThat(api.put("/api/projects/" + projectId, projectBody).getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map> meals = (List<Map>) api.get("/api/projects/" + projectId).getBody().get("meals");
        assertThat(meals).extracting(item -> item.get("id")).containsExactly(lunch, dinner);
    }

    @Test
    @DisplayName("resposta da obra deve listar os restaurantes, e obra com restaurante nao pode ser excluida")
    void projectListsRestaurantsAndBlocksDelete() {
        String projectId = api.createProject();
        String restaurantId = api.create("/api/restaurants", api.restaurantBody(projectId));

        List<Map> restaurants = (List<Map>) api.get("/api/projects/" + projectId).getBody().get("restaurants");
        ResponseEntity<Map> delete = api.delete("/api/projects/" + projectId);

        assertThat(restaurants).extracting(item -> item.get("id")).containsExactly(restaurantId);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(delete.getBody()).containsEntry("errorCode", "project.in_use");
        assertThat((String) delete.getBody().get("message")).contains("restaurants");
    }

    @Test
    @DisplayName("criar, alterar e excluir refeicao e restaurante nao deve mudar updatedAt da obra")
    void mealChangesDoNotTouchProjectTimestamp() {
        String projectId = api.createProject();
        Object updatedAt = api.get("/api/projects/" + projectId).getBody().get("updatedAt");

        Map<String, Object> restaurant = api.restaurantBody(projectId);
        String restaurantId = api.create("/api/restaurants", restaurant);
        Map<String, Object> meal = api.mealBody(restaurantId, "LUNCH");
        String mealId = api.create("/api/meals", meal);
        meal.put("lunchQuantity", 20);
        api.put("/api/meals/" + mealId, meal);
        api.delete("/api/meals/" + mealId);
        restaurant.put("name", AdminApi.unique("Restaurante Alterado "));
        api.put("/api/restaurants/" + restaurantId, restaurant);

        assertThat(api.get("/api/projects/" + projectId).getBody().get("updatedAt")).isEqualTo(updatedAt);
    }

    private long total() {
        return ((Number) api.get("/api/restaurants?size=1").getBody().get("totalElements")).longValue();
    }
}
