package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.core.exception.InvalidRestaurantException;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.core.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.addressRequest;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.restaurant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestaurantUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC);

    private RestaurantRepository restaurantRepository;
    private ProjectRepository projectRepository;
    private MealRepository mealRepository;
    private RestaurantUseCase useCase;

    @BeforeEach
    void setUp() {
        restaurantRepository = mock(RestaurantRepository.class);
        projectRepository = mock(ProjectRepository.class);
        mealRepository = mock(MealRepository.class);
        useCase = new RestaurantUseCase(restaurantRepository, projectRepository, mealRepository, CLOCK);
    }

    @Test
    void shouldListPageWithMealsLoadedInOneQuery() {
        Project project = project();
        Restaurant withMeal = restaurant(project, new BigDecimal("8.00"), new BigDecimal("5.00"));
        Restaurant withoutMeal = restaurant(project);
        Meal meal = new Meal(null, withMeal, MealType.LUNCH_AND_DINNER, false, 10, new BigDecimal("25.00"),
                8, new BigDecimal("22.00"));
        PageRequest request = new PageRequest(0, 20);
        when(restaurantRepository.findAll(request)).thenReturn(new Page<>(List.of(withMeal, withoutMeal), 0, 20, 2, 1));
        when(mealRepository.findByRestaurantIn(List.of(withMeal, withoutMeal))).thenReturn(List.of(meal));

        PageResponseDTO<RestaurantResponseDTO> page = useCase.findAll(request);

        verify(mealRepository, times(1)).findByRestaurantIn(anyCollection());
        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).meals()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(meal.getId());
            assertThat(summary.dailyPrice()).isEqualByComparingTo("452.00");
            assertThat(summary.projections()).hasSize(4);
        });
        assertThat(page.content().get(1).meals()).isEmpty();
    }

    @Test
    void shouldCreateWithDeliveryFeeAndNoMeals() {
        Project project = project();
        when(projectRepository.findById(project.getId())).thenReturn(project);
        when(restaurantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantResponseDTO response = useCase.create(new RestaurantRequestDTO("Cantina", null, addressRequest(),
                project.getId(), new BigDecimal("8.00"), new BigDecimal("10.00"), new BigDecimal("5.00")));

        assertThat(response.lunchDeliveryFee()).isEqualByComparingTo("8.00");
        assertThat(response.dinnerDeliveryFee()).isEqualByComparingTo("10.00");
        assertThat(response.sodaPrice()).isEqualByComparingTo("5.00");
        assertThat(response.meals()).isEmpty();
        verify(mealRepository, never()).findByRestaurantIn(anyCollection());
    }

    @Test
    void shouldRejectInvalidDeliveryFeeWithoutSaving() {
        Project project = project();
        when(projectRepository.findById(project.getId())).thenReturn(project);

        assertThatThrownBy(() -> useCase.create(new RestaurantRequestDTO("Cantina", null, null, project.getId(),
                new BigDecimal("8.00"), BigDecimal.ZERO, null)))
                .isInstanceOf(InvalidRestaurantException.class)
                .extracting("errorCode").isEqualTo("restaurant.invalid.delivery_fee");
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void shouldReturnMealsOnFindById() {
        Restaurant restaurant = restaurant(project());
        Meal meal = new Meal(null, restaurant, MealType.DINNER, false, null, null, 8, new BigDecimal("22.00"));
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(restaurant);
        when(mealRepository.findByRestaurantIn(List.of(restaurant))).thenReturn(List.of(meal));

        RestaurantResponseDTO response = useCase.findById(restaurant.getId());

        assertThat(response.meals()).extracting(summary -> summary.id()).containsExactly(meal.getId());
    }
}
