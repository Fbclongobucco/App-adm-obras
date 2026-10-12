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
class EmployeeCostCenterIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private AdminApi api;

    @BeforeEach
    void setUp() {
        api = new AdminApi(rest);
    }

    @Test
    @DisplayName("trocar o centro de custo deve mover o colaborador sem alterar nenhum centro de custo")
    void changingCostCenterMovesEmployee() {
        String centerA = api.createCostCenter();
        String centerB = api.createCostCenter();
        Map before = Map.of("A", api.get("/api/cost-centers/" + centerA).getBody(),
                "B", api.get("/api/cost-centers/" + centerB).getBody());
        Map<String, Object> employee = api.employeeBody(centerA);
        String employeeId = api.create("/api/employees", employee);

        employee.put("costCenterId", centerB);
        ResponseEntity<Map> updated = api.put("/api/employees/" + employeeId, employee);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map costCenter = (Map) api.get("/api/employees/" + employeeId).getBody().get("costCenter");
        assertThat(costCenter.get("id")).isEqualTo(centerB);
        assertThat(ids("/api/employees/by-cost-center/" + centerB)).contains(employeeId);
        assertThat(ids("/api/employees/by-cost-center/" + centerA)).doesNotContain(employeeId);
        assertSameNameAndCnpj(api.get("/api/cost-centers/" + centerA).getBody(), (Map) before.get("A"));
        assertSameNameAndCnpj(api.get("/api/cost-centers/" + centerB).getBody(), (Map) before.get("B"));
    }

    @Test
    @DisplayName("centro de custo inexistente deve responder 404 e manter o colaborador no centro anterior")
    void unknownCostCenterIsRejected() {
        String centerA = api.createCostCenter();
        Map<String, Object> employee = api.employeeBody(centerA);
        String employeeId = api.create("/api/employees", employee);

        employee.put("costCenterId", UUID.randomUUID().toString());
        ResponseEntity<Map> response = api.put("/api/employees/" + employeeId, employee);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("errorCode", "cost_center.not_found");
        assertThat(ids("/api/employees/by-cost-center/" + centerA)).contains(employeeId);
    }

    private List<Object> ids(String path) {
        return ((List<Map>) api.getList(path).getBody()).stream().map(item -> item.get("id")).toList();
    }

    private static void assertSameNameAndCnpj(Map actual, Map expected) {
        assertThat(actual.get("name")).isEqualTo(expected.get("name"));
        assertThat(actual.get("cnpj")).isEqualTo(expected.get("cnpj"));
    }
}
