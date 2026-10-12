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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Busca q nas listagens (change busca-nas-listagens). O H2 e compartilhado entre as classes de
 * teste, entao cada cenario usa um marcador unico no texto pesquisado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class ListSearchIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    /** Marcador so com letras, para nao casar com CPF ou CNPJ pelos digitos. */
    private static String marker() {
        String digits = AdminApi.unique("");
        StringBuilder letters = new StringBuilder();
        for (char c : digits.substring(Math.max(0, digits.length() - 10)).toCharArray()) {
            letters.append((char) ('a' + (c - '0')));
        }
        return letters.toString();
    }

    /** O TestRestTemplate codifica a URL (espacos, acentos e %). */
    private Map search(String path, String q) {
        String separator = path.contains("?") ? "&" : "?";
        ResponseEntity<Map> response = api.get(path + separator + "q=" + q);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static List<Object> ids(Map page) {
        return ((List<Map>) page.get("content")).stream().map(item -> item.get("id")).toList();
    }

    @Test
    @DisplayName("colaborador por nome sem acento e por CPF formatado")
    void employeeByNameWithoutAccentsAndFormattedCpf() {
        String mark = marker();
        Map<String, Object> body = api.employeeBody(api.createCostCenter());
        body.put("name", "José Antônio " + mark);
        String id = api.create("/api/employees", body);
        String cpf = (String) body.get("cpf");

        Map byName = search("/api/employees", "JOSE ANTONIO " + mark);
        assertThat(ids(byName)).containsExactly(id);
        assertThat(byName.get("totalElements")).isEqualTo(1);

        String formatted = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        assertThat(ids(search("/api/employees?size=100", formatted))).contains(id);
    }

    @Test
    @DisplayName("clientes: total filtrado, paginacao do resultado e cidade do endereco")
    void clientsFilteredTotalPaginationAndCity() {
        String mark = marker();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> body = api.clientBody();
            body.put("name", "Fundição " + mark + " " + i);
            api.create("/api/clients", body);
        }
        api.createClient();

        Map all = search("/api/clients", "fundicao " + mark);
        assertThat(all.get("totalElements")).isEqualTo(3);

        Map second = search("/api/clients?page=1&size=2", "fundicao " + mark);
        assertThat((List) second.get("content")).hasSize(1);
        assertThat(second.get("totalElements")).isEqualTo(3);

        Map<String, Object> withCity = api.clientBody();
        Map<String, Object> address = AdminApi.address();
        address.put("city", "Itaguaçu " + mark);
        withCity.put("address", address);
        String id = api.create("/api/clients", withCity);
        assertThat(ids(search("/api/clients", "itaguacu " + mark))).containsExactly(id);
    }

    @Test
    @DisplayName("obra pelo nome do cliente e pela OS")
    void projectByClientNameAndOs() {
        String mark = marker();
        Map<String, Object> client = api.clientBody();
        client.put("name", "Agroindustrial Campo Novo " + mark);
        Map<String, Object> project = api.projectBody(api.createCostCenter(), api.create("/api/clients", client));
        String id = api.create("/api/projects", project);

        assertThat(ids(search("/api/projects", "campo novo " + mark))).containsExactly(id);
        assertThat(ids(search("/api/projects?sort=os,desc", (String) project.get("os")))).containsExactly(id);
    }

    @Test
    @DisplayName("centro de custo pelo nome e pelo CNPJ formatado")
    void costCenterByNameAndCnpj() {
        String mark = marker();
        Map<String, Object> body = api.costCenterBody();
        body.put("name", "Centro Elétrico " + mark);
        String id = api.create("/api/cost-centers", body);
        String cnpj = (String) body.get("cnpj");

        assertThat(ids(search("/api/cost-centers", "eletrico " + mark))).containsExactly(id);
        String formatted = cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12);
        assertThat(ids(search("/api/cost-centers?size=100", formatted))).contains(id);
    }

    @Test
    @DisplayName("restaurante sem endereco pelo nome, e restaurante e refeicao pela OS da obra")
    void restaurantAndMealByNameAndProjectOs() {
        String mark = marker();
        Map<String, Object> projectBody = api.projectBody(api.createCostCenter(), api.createClient());
        String projectId = api.create("/api/projects", projectBody);
        Map<String, Object> restaurant = api.restaurantBody(projectId);
        restaurant.put("name", "Cantina Boa Mesa " + mark);
        restaurant.put("address", null);
        String restaurantId = api.create("/api/restaurants", restaurant);
        String mealId = api.create("/api/meals", api.mealBody(restaurantId, "LUNCH"));
        String os = (String) projectBody.get("os");

        assertThat(ids(search("/api/restaurants", "boa mesa " + mark))).containsExactly(restaurantId);
        assertThat(ids(search("/api/restaurants", os))).containsExactly(restaurantId);
        assertThat(ids(search("/api/meals", os))).containsExactly(mealId);
        assertThat(ids(search("/api/meals", "cantina boa mesa " + mark))).containsExactly(mealId);
    }

    @Test
    @DisplayName("hospedagem pelo anfitriao e pela OS da obra")
    void accommodationByHostAndProjectOs() {
        String mark = marker();
        Map<String, Object> projectBody = api.projectBody(api.createCostCenter(), api.createClient());
        String projectId = api.create("/api/projects", projectBody);
        Map<String, Object> body = api.accommodationBody(projectId);
        body.put("hostName", "Pousada São João " + mark);
        String id = api.create("/api/accommodations", body);

        assertThat(ids(search("/api/accommodations", "sao joao " + mark))).containsExactly(id);
        assertThat(ids(search("/api/accommodations", (String) projectBody.get("os")))).containsExactly(id);
    }

    @Test
    @DisplayName("usuario pelo e-mail; sem q ou q em branco nao filtra")
    void usersByEmailAndBlankQuery() {
        Map found = search("/api/users", "ADMIN@ADMIN");
        assertThat((List<Map>) found.get("content")).extracting(user -> user.get("email")).contains("admin@admin.com");

        Map all = api.get("/api/users").getBody();
        assertThat(search("/api/users", "   ").get("totalElements")).isEqualTo(all.get("totalElements"));
    }

    @Test
    @DisplayName("curingas % e _ sao literais")
    void wildcardsAreLiteral() {
        assertThat(search("/api/clients", "%").get("totalElements")).isEqualTo(0);
        assertThat(search("/api/clients", "_").get("totalElements")).isEqualTo(0);
    }

    @Test
    @DisplayName("termo com mais de 100 caracteres deve retornar 400 page.invalid.query")
    void tooLongQuery() {
        ResponseEntity<Map> response = api.get("/api/projects?q=" + "a".repeat(101));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("errorCode", "page.invalid.query");
    }
}
