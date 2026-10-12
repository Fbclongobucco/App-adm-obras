package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.meal.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.exception.InvalidMealException;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.core.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.meal;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.mealRequest;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.restaurant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealUseCaseTest {

    static final Instant CLOCK_NOW = Instant.parse("2026-01-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(CLOCK_NOW, ZoneOffset.UTC);

    private MealRepository mealRepository;
    private ProjectRepository projectRepository;
    private RestaurantRepository restaurantRepository;
    private MealUseCase useCase;

    @BeforeEach
    void setUp() {
        mealRepository = mock(MealRepository.class);
        projectRepository = mock(ProjectRepository.class);
        restaurantRepository = mock(RestaurantRepository.class);
        useCase = new MealUseCase(mealRepository, projectRepository, restaurantRepository, CLOCK);
    }

    @Test
    void shouldCreateMealWithTheRestaurantProject() {
        Project project = project();
        Restaurant restaurant = restaurant(project);
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(restaurant);
        when(mealRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MealResponseDTO response = useCase.create(mealRequest(restaurant.getId(), MealType.LUNCH));

        assertThat(response.restaurant().id()).isEqualTo(restaurant.getId());
        assertThat(response.project().id()).isEqualTo(project.getId());
        assertThat(response.project().client().name()).isEqualTo("Construtora Alfa");
    }

    @Test
    void shouldRejectCreateWithUnknownRestaurant() {
        assertThatThrownBy(() -> useCase.create(mealRequest(UUID.randomUUID(), MealType.LUNCH)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("restaurant.not_found");
        verify(mealRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWithoutRestaurant() {
        assertThatThrownBy(() -> useCase.create(mealRequest(null, MealType.LUNCH)))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.restaurant");
        verify(mealRepository, never()).save(any());
    }

    @Test
    void shouldRejectSecondMealOfTheSameTypeInTheProject() {
        Project project = project();
        Restaurant restaurant = restaurant(project);
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(restaurant);
        when(mealRepository.findByProject(project)).thenReturn(List.of(meal(MealType.LUNCH, project)));

        assertThatThrownBy(() -> useCase.create(mealRequest(restaurant.getId(), MealType.LUNCH)))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("meal.duplicate.type");
        verify(mealRepository, never()).save(any());
    }

    @Test
    void shouldAcceptOtherTypeInTheSameProject() {
        Project project = project();
        Restaurant restaurant = restaurant(project);
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(restaurant);
        when(mealRepository.findByProject(project)).thenReturn(List.of(meal(MealType.LUNCH, project)));
        when(mealRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(useCase.create(mealRequest(restaurant.getId(), MealType.DINNER)).mealType())
                .isEqualTo(MealType.DINNER);
    }

    @Test
    void shouldFindByProjectId() {
        Project project = project();
        when(projectRepository.findById(project.getId())).thenReturn(project);
        when(mealRepository.findByProject(project)).thenReturn(List.of(meal(MealType.LUNCH)));

        assertThat(useCase.findByProjectId(project.getId())).hasSize(1);
    }
}
