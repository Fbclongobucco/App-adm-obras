package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.project.ProjectResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.exception.InvalidProjectException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.accommodation;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.client;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenter;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.employee;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.meal;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.projectRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMapperTest {

    @Test
    void shouldMapRequestWithRelationsToDomain() {
        CostCenter costCenter = costCenter();
        Client client = client();
        Accommodation accommodation = accommodation();
        Employee employee = employee();
        Meal lunch = meal(MealType.LUNCH);
        Meal dinner = meal(MealType.DINNER);

        Project project = ProjectMapper.toDomain(projectRequest(null, null, null, null, null, null), costCenter,
                client, Set.of(accommodation), Set.of(employee), lunch, dinner);

        assertThat(project.getOs()).isEqualTo("OS-1234");
        assertThat(project.getCostCenter()).isSameAs(costCenter);
        assertThat(project.getClient()).isSameAs(client);
        assertThat(project.getAccommodations()).containsExactly(accommodation);
        assertThat(project.getEmployees()).containsExactly(employee);
        assertThat(project.getLunch()).isSameAs(lunch);
        assertThat(project.getDinner()).isSameAs(dinner);
        assertThat(project.getTotalPrice()).isEqualByComparingTo("1500.00");
    }

    @Test
    void shouldRejectMealOnWrongSlot() {
        assertThatThrownBy(() -> ProjectMapper.toDomain(projectRequest(null, null, null, null, null, null),
                costCenter(), client(), null, null, meal(MealType.DINNER), null))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.meal_type");
    }

    @Test
    void shouldKeepIdWhenMappingOverExisting() {
        Project existing = ProjectMapper.toDomain(projectRequest(null, null, null, null, null, null),
                costCenter(), client(), null, null, null, null);

        Project updated = ProjectMapper.toDomain(existing, projectRequest(null, null, null, null, null, null),
                costCenter(), client(), null, null, null, null);

        assertThat(updated.getId()).isEqualTo(existing.getId());
    }

    @Test
    void shouldMapDomainToResponseWithNestedObjects() {
        Accommodation accommodation = accommodation();
        Employee employee = employee();
        Project project = ProjectMapper.toDomain(projectRequest(null, null, null, null, null, null), costCenter(),
                client(), Set.of(accommodation), Set.of(employee), meal(MealType.LUNCH), null);

        ProjectResponseDTO response = ProjectMapper.toResponse(project);

        assertThat(response.costCenter().name()).isEqualTo("Obra Sao Paulo");
        assertThat(response.client().name()).isEqualTo("Construtora Alfa");
        assertThat(response.client().address().state()).isEqualTo("SP");
        assertThat(response.accommodations()).singleElement()
                .satisfies(summary -> assertThat(summary.hostName()).isEqualTo("Maria Souza"));
        assertThat(response.employees()).singleElement()
                .satisfies(summary -> assertThat(summary.id()).isEqualTo(employee.getId()));
        assertThat(response.lunch().restaurantName()).isEqualTo("Restaurante do Ze");
        assertThat(response.dinner()).isNull();
        assertThat(response.totalPrice()).isEqualByComparingTo("1250.00");
    }

    @Test
    void shouldMapSummaryWithoutCollections() {
        ProjectSummaryDTO summary = ProjectMapper.toSummary(ProjectMapper.toDomain(
                projectRequest(null, null, null, null, null, null), costCenter(), client(), null, null, null, null));

        assertThat(summary.os()).isEqualTo("OS-1234");
        assertThat(summary.client().name()).isEqualTo("Construtora Alfa");
        assertThat(ProjectMapper.toSummary(null)).isNull();
    }
}
