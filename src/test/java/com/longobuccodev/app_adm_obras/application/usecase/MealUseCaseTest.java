package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.client;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenter;
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
    void shouldRejectLunchOrDinnerWhenProjectHasLunchAndDinner() {
        Project project = project();
        Restaurant restaurant = restaurant(project);
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(restaurant);
        when(mealRepository.findByProject(project))
                .thenReturn(List.of(meal(MealType.LUNCH_AND_DINNER, project)));

        for (MealType type : MealType.values()) {
            assertThatThrownBy(() -> useCase.create(mealRequest(restaurant.getId(), type)))
                    .isInstanceOf(ConflictException.class)
                    .extracting("errorCode").isEqualTo("meal.duplicate.type");
        }
        verify(mealRepository, never()).save(any());
    }

    @Test
    void updateShouldIgnoreItselfWhenCheckingCoverage() {
        Project project = project();
        Restaurant restaurant = restaurant(project);
        Meal existing = meal(MealType.LUNCH, project);
        existing.markCreated(CLOCK_NOW);
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(restaurant);
        when(mealRepository.findById(existing.getId())).thenReturn(existing);
        when(mealRepository.findByProject(project)).thenReturn(List.of(existing));

        MealResponseDTO response = useCase.update(existing.getId(),
                mealRequest(restaurant.getId(), MealType.LUNCH_AND_DINNER));

        assertThat(response.mealType()).isEqualTo(MealType.LUNCH_AND_DINNER);
        assertThat(response.dailyPrice()).isEqualByComparingTo("426.00");
        verify(mealRepository).update(any(Meal.class));
    }

    @Test
    void shouldFindByProjectId() {
        Project project = project();
        when(projectRepository.findById(project.getId())).thenReturn(project);
        when(mealRepository.findByProject(project)).thenReturn(List.of(meal(MealType.LUNCH)));

        assertThat(useCase.findByProjectId(project.getId())).hasSize(1);
    }

    @Test
    void shouldAddAdjustmentThroughTheMealAndSaveIt() {
        Meal meal = mealWithTwoWeekProject();
        when(mealRepository.findById(meal.getId())).thenReturn(meal);

        MealAdjustmentResponseDTO response = useCase.addAdjustment(meal.getId(), new MealAdjustmentRequestDTO(
                LocalDate.of(2026, 10, 12), MealAdjustment.Type.NO_SERVICE, null, null, null, null, null, "Feriado"));

        assertThat(response.type()).isEqualTo(MealAdjustment.Type.NO_SERVICE);
        assertThat(response.inPeriod()).isTrue();
        assertThat(meal.getAdjustments()).singleElement().satisfies(adjustment -> {
            assertThat(adjustment.getId()).isEqualTo(response.id());
            assertThat(adjustment.getCreatedAt()).isEqualTo(CLOCK_NOW);
        });
        assertThat(meal.getUpdatedAt()).isEqualTo(CLOCK_NOW);
        verify(mealRepository).update(meal);
    }

    @Test
    void shouldNotSaveInvalidAdjustment() {
        Meal meal = mealWithTwoWeekProject();
        when(mealRepository.findById(meal.getId())).thenReturn(meal);

        assertThatThrownBy(() -> useCase.addAdjustment(meal.getId(), new MealAdjustmentRequestDTO(
                LocalDate.of(2026, 10, 7), MealAdjustment.Type.EXTRA_DAY, 6, 0, null, null, null, null)))
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.extra_day");
        verify(mealRepository, never()).update(any());
    }

    @Test
    void shouldRejectAdjustmentOfUnknownMeal() {
        assertThatThrownBy(() -> useCase.findAdjustments(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("meal.not_found");
    }

    @Test
    void shouldRemoveOnlyAdjustmentOfTheMeal() {
        Meal meal = mealWithTwoWeekProject();
        MealAdjustment holiday = meal.addAdjustment(new MealAdjustment(null, LocalDate.of(2026, 10, 12),
                MealAdjustment.Type.NO_SERVICE, null, null, null));
        when(mealRepository.findById(meal.getId())).thenReturn(meal);

        assertThatThrownBy(() -> useCase.removeAdjustment(meal.getId(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("meal.adjustment.not_found");
        verify(mealRepository, never()).update(any());

        useCase.removeAdjustment(meal.getId(), holiday.getId());

        assertThat(meal.getAdjustments()).isEmpty();
        verify(mealRepository).update(meal);
    }

    @Test
    void updateShouldKeepAdjustments() {
        Meal existing = mealWithTwoWeekProject();
        existing.markCreated(CLOCK_NOW);
        MealAdjustment holiday = existing.addAdjustment(new MealAdjustment(null, LocalDate.of(2026, 10, 12),
                MealAdjustment.Type.NO_SERVICE, null, null, null));
        Restaurant restaurant = existing.getRestaurant();
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(restaurant);
        when(mealRepository.findById(existing.getId())).thenReturn(existing);
        when(mealRepository.findByProject(existing.getProject())).thenReturn(List.of(existing));

        MealResponseDTO response = useCase.update(existing.getId(), mealRequest(restaurant.getId(), MealType.LUNCH));

        assertThat(response.adjustments()).extracting(MealAdjustmentResponseDTO::id).containsExactly(holiday.getId());
        assertThat(response.serviceDays()).isEqualTo(9);
    }

    private static Meal mealWithTwoWeekProject() {
        Project project = new Project(null, "OS-2026", null, costCenter(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 16), client(), false);
        return meal(MealType.LUNCH_AND_DINNER, project);
    }
}
