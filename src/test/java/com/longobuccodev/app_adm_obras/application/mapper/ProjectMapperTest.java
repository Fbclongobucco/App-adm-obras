package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.project.ProjectResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.accommodation;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.client;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenter;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.employee;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.meal;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.projectRequest;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectMapperTest {

    @Test
    void shouldMapRequestWithRelationsToDomain() {
        CostCenter costCenter = costCenter();
        Client client = client();
        Accommodation accommodation = accommodation();
        Employee employee = employee();

        Project project = ProjectMapper.toDomain(projectRequest(null, null, null, null), costCenter,
                client, Set.of(accommodation), Set.of(employee));

        assertThat(project.getOs()).isEqualTo("OS-1234");
        assertThat(project.getCostCenter()).isSameAs(costCenter);
        assertThat(project.getClient()).isSameAs(client);
        assertThat(project.getAccommodations()).containsExactly(accommodation);
        assertThat(project.getEmployees()).containsExactly(employee);
        assertThat(project.getMeals()).isEmpty();
        assertThat(project.getTotalPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldKeepIdWhenMappingOverExisting() {
        Project existing = ProjectMapper.toDomain(projectRequest(null, null, null, null),
                costCenter(), client(), null, null);

        Project updated = ProjectMapper.toDomain(existing, projectRequest(null, null, null, null),
                costCenter(), client(), null, null);

        assertThat(updated.getId()).isEqualTo(existing.getId());
    }

    @Test
    void shouldMapDomainToResponseWithNestedObjects() {
        Accommodation accommodation = accommodation();
        Employee employee = employee();
        Project project = new Project(null, "OS-1234", null, costCenter(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 16), client(), false);
        project.addAccommodation(accommodation);
        project.addEmployee(employee);
        project.addMeal(meal(MealType.LUNCH_AND_DINNER, project));

        ProjectResponseDTO response = ProjectMapper.toResponse(project);

        assertThat(response.costCenter().name()).isEqualTo("Obra Sao Paulo");
        assertThat(response.client().name()).isEqualTo("Construtora Alfa");
        assertThat(response.client().address().state()).isEqualTo("SP");
        assertThat(response.accommodations()).singleElement()
                .satisfies(summary -> assertThat(summary.hostName()).isEqualTo("Maria Souza"));
        assertThat(response.employees()).singleElement()
                .satisfies(summary -> assertThat(summary.id()).isEqualTo(employee.getId()));
        assertThat(response.meals()).singleElement().satisfies(meal -> {
            assertThat(meal.restaurant().name()).isEqualTo("Restaurante do Ze");
            assertThat(meal.mealType()).isEqualTo(MealType.LUNCH_AND_DINNER);
            assertThat(meal.dailyPrice()).isEqualByComparingTo("426.00");
            assertThat(meal.workingDays()).isEqualTo(10);
            assertThat(meal.totalPrice()).isEqualByComparingTo("4260.00");
        });
        // 4260.00 das refeicoes + 1000.00 da hospedagem
        assertThat(response.totalPrice()).isEqualByComparingTo("5260.00");
    }

    @Test
    void shouldMapSummaryWithoutCollections() {
        ProjectSummaryDTO summary = ProjectMapper.toSummary(ProjectMapper.toDomain(
                projectRequest(null, null, null, null), costCenter(), client(), null, null));

        assertThat(summary.os()).isEqualTo("OS-1234");
        assertThat(summary.client().name()).isEqualTo("Construtora Alfa");
        assertThat(ProjectMapper.toSummary(null)).isNull();
    }
}
