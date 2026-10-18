package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.addressRequest;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.restaurant;
import static org.assertj.core.api.Assertions.assertThat;

class RestaurantMapperTest {

    @Test
    void shouldMapDeliveryFeeFromRequestAndKeepIdOnUpdate() {
        Project project = project();
        RestaurantRequestDTO request = new RestaurantRequestDTO("Cantina", "1933336666", addressRequest(),
                project.getId(), new BigDecimal("8.00"), new BigDecimal("10.00"), new BigDecimal("5.00"));

        Restaurant created = RestaurantMapper.toDomain(request, project);
        Restaurant updated = RestaurantMapper.toDomain(created,
                new RestaurantRequestDTO("Cantina", null, null, project.getId(), null, null, null), project);

        assertThat(created.getLunchDeliveryFee()).isEqualByComparingTo("8.00");
        assertThat(created.getDinnerDeliveryFee()).isEqualByComparingTo("10.00");
        assertThat(created.getSodaPrice()).isEqualByComparingTo("5.00");
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getLunchDeliveryFee()).isNull();
        assertThat(updated.getDinnerDeliveryFee()).isNull();
        assertThat(updated.getSodaPrice()).isNull();
    }

    @Test
    void shouldMapResponseWithFeeAndMeals() {
        Restaurant restaurant = restaurant(project(), new BigDecimal("8.00"), new BigDecimal("5.00"));
        Meal meal = new Meal(null, restaurant, MealType.LUNCH, false, 10, new BigDecimal("25.00"), null, null);

        RestaurantResponseDTO response = RestaurantMapper.toResponse(restaurant, List.of(meal));
        RestaurantResponseDTO empty = RestaurantMapper.toResponse(restaurant, null);

        assertThat(response.lunchDeliveryFee()).isEqualByComparingTo("8.00");
        assertThat(response.dinnerDeliveryFee()).isEqualByComparingTo("8.00");
        assertThat(response.sodaPrice()).isEqualByComparingTo("5.00");
        assertThat(response.meals()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(meal.getId());
            assertThat(summary.restaurant().lunchDeliveryFee()).isEqualByComparingTo("8.00");
            assertThat(summary.restaurant().sodaPrice()).isEqualByComparingTo("5.00");
            assertThat(summary.sodaPrice()).isEqualByComparingTo("5.00");
            assertThat(summary.dailyPrice()).isEqualByComparingTo("263.00");
        });
        assertThat(empty.meals()).isEmpty();
        assertThat(RestaurantMapper.toSummary(restaurant).dinnerDeliveryFee()).isEqualByComparingTo("8.00");
        assertThat(RestaurantMapper.toSummary(restaurant).sodaPrice()).isEqualByComparingTo("5.00");
    }
}
