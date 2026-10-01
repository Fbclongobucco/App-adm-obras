package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.MealResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import org.junit.jupiter.api.Test;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.mealRequest;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static org.assertj.core.api.Assertions.assertThat;

class MealMapperTest {

    @Test
    void shouldMapDomainToResponseWithProject() {
        Project project = project();
        Meal meal = MealMapper.toDomain(mealRequest(null, MealType.LUNCH), project);

        MealResponseDTO response = MealMapper.toResponse(meal);

        assertThat(response.project().id()).isEqualTo(project.getId());
        assertThat(response.project().os()).isEqualTo("OS-1234");
        assertThat(response.mealType()).isEqualTo(MealType.LUNCH);
        assertThat(response.totalPrice()).isEqualByComparingTo("250.00");
    }

    @Test
    void shouldKeepIdWhenMappingOverExisting() {
        Meal existing = MealMapper.toDomain(mealRequest(null, MealType.LUNCH), null);

        Meal updated = MealMapper.toDomain(existing, mealRequest(null, MealType.DINNER), null);

        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getMealType()).isEqualTo(MealType.DINNER);
    }
}
