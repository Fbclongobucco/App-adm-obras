package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.exception.InvalidProjectException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectTest {

    private static Client newClient() {
        return new Client(null, "Construtora Alfa", "contato@alfa.com", "1133334444",
                new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"));
    }

    private static CostCenter newCostCenter() {
        return new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81");
    }

    /** Almoco 10 x 25.00 e jantar 8 x 22.00, conforme o tipo; restaurante de outra instancia da obra. */
    private static Meal newMeal(MealType mealType) {
        Project owner = new Project(null, "OS-9999", null, newCostCenter(), LocalDate.of(2026, 1, 10), null,
                new Client(null, "Construtora Alfa", "contato@alfa.com", "1133334444",
                        new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000")),
                false);
        return new Meal(null, new Restaurant(null, "Restaurante do Ze", null, null, owner), mealType, false,
                10, new BigDecimal("25.00"), 8, new BigDecimal("22.00"));
    }

    /** Segunda 2026-10-05 a sexta 2026-10-16: 10 dias uteis. */
    private static Project twoWeekProject() {
        return new Project(null, "OS-2026", null, newCostCenter(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 16), newClient(), false);
    }

    private static Project newProject() {
        return new Project(null, " os-1234 ", "Obra de reforma ", newCostCenter(),
                LocalDate.of(2026, 1, 10), null,
                newClient(), null);
    }

    @Test
    void shouldNormalizeFieldsOnConstructor() {
        Project project = newProject();

        assertThat(project.getId()).isNotNull();
        assertThat(project.getOs()).isEqualTo("OS-1234");
        assertThat(project.getDescription()).isEqualTo("Obra de reforma");
        assertThat(project.getAccommodations()).isEmpty();
        assertThat(project.getEmployees()).isEmpty();
        assertThat(project.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(project.getIsCompleted()).isFalse();
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();

        assertThat(new Project(id, "OS-1234", "Obra de reforma", newCostCenter(),
                LocalDate.of(2026, 1, 10), null,
                newClient(), false).getId()).isEqualTo(id);
        assertThat(newProject().getId()).isNotNull();
    }

    @Test
    void shouldAcceptNullDescriptionAndEndDate() {
        Project project = newProject();

        project.setDescription(null);
        project.setEndDate(null);

        assertThat(project.getDescription()).isNull();
        assertThat(project.getEndDate()).isNull();
    }

    @Test
    void shouldRejectBlankOs() {
        Project project = newProject();

        assertThatThrownBy(() -> project.setOs(" "))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.os.blank");
    }

    @Test
    void shouldRejectInvalidOs() {
        Project project = newProject();

        assertThatThrownBy(() -> project.setOs("OS"))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.os");
        assertThatThrownBy(() -> project.setOs("OS 1234"))
                .isInstanceOf(InvalidProjectException.class);
    }

    @Test
    void shouldRejectInvalidDescription() {
        Project project = newProject();

        assertThatThrownBy(() -> project.setDescription("ab"))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.description.length");
        assertThatThrownBy(() -> project.setDescription("a".repeat(501)))
                .isInstanceOf(InvalidProjectException.class);
    }

    @Test
    void shouldRejectMissingCostCenter() {
        Project project = newProject();

        assertThatThrownBy(() -> project.setCostCenter(null))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.cost_center");
    }

    @Test
    void shouldRejectMissingStartDate() {
        Project project = newProject();

        assertThatThrownBy(() -> project.setStartDate(null))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.start_date");
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {
        Project project = newProject();

        assertThatThrownBy(() -> project.setEndDate(LocalDate.of(2026, 1, 9)))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.end_date");
    }

    @Test
    void shouldAcceptEndDateOnStartDate() {
        Project project = newProject();

        project.setEndDate(LocalDate.of(2026, 1, 10));

        assertThat(project.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    void shouldRejectMissingClient() {
        Project project = newProject();

        assertThatThrownBy(() -> project.setClient(null))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.client");
    }

    @Test
    void shouldHoldLunchAndDinnerMealsAndLinkThemToItself() {
        Project project = twoWeekProject();
        Meal dinner = newMeal(MealType.DINNER);
        Meal lunch = newMeal(MealType.LUNCH);

        project.addMeal(dinner);
        project.addMeal(lunch);

        assertThat(project.getMeals()).containsExactly(lunch, dinner);
        assertThat(lunch.getProject()).isSameAs(project);
        assertThat(dinner.getProject()).isSameAs(project);
        // (10 x 25.00 + 8 x 22.00) x 10 dias uteis
        assertThat(project.getTotalPrice()).isEqualByComparingTo("4260.00");
    }

    @Test
    void shouldRejectOverlappingMealCoverage() {
        Project project = newProject();
        project.addMeal(newMeal(MealType.LUNCH_AND_DINNER));

        assertThatThrownBy(() -> project.addMeal(newMeal(MealType.LUNCH)))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.duplicated_meal_type");
        assertThatThrownBy(() -> project.addMeal(newMeal(MealType.DINNER)))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.duplicated_meal_type");
        assertThat(project.getMeals()).hasSize(1);
    }

    @Test
    void shouldAcceptSameMealAgain() {
        Project project = newProject();
        Meal lunch = newMeal(MealType.LUNCH);
        project.addMeal(lunch);

        project.addMeal(lunch);

        assertThat(project.getMeals()).containsExactly(lunch);
    }

    @Test
    void shouldRejectNullMeal() {
        Project project = newProject();

        assertThatThrownBy(() -> project.addMeal(null))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.meal");
    }

    @Test
    void shouldRemoveMealFromTotal() {
        Project project = twoWeekProject();
        Meal lunch = newMeal(MealType.LUNCH);
        project.addMeal(lunch);
        project.addMeal(newMeal(MealType.DINNER));

        project.removeMeal(lunch);

        assertThat(project.getMeals()).hasSize(1);
        assertThat(project.getTotalPrice()).isEqualByComparingTo("1760.00");
    }

    @Test
    void shouldCountWeekdaysBetweenStartAndEndInclusive() {
        assertThat(twoWeekProject().getWorkingDays()).isEqualTo(10);
        assertThat(projectBetween("2026-10-05", "2026-10-05").getWorkingDays()).isEqualTo(1);
        // sabado a domingo
        assertThat(projectBetween("2026-10-10", "2026-10-11").getWorkingDays()).isZero();
        // sabado a sabado: segunda a sexta de uma semana
        assertThat(projectBetween("2026-10-10", "2026-10-17").getWorkingDays()).isEqualTo(5);
        // quarta a terca seguinte
        assertThat(projectBetween("2026-10-07", "2026-10-13").getWorkingDays()).isEqualTo(5);
        // outubro de 2026 inteiro: 22 dias uteis
        assertThat(projectBetween("2026-10-01", "2026-10-31").getWorkingDays()).isEqualTo(22);
    }

    @Test
    void shouldHaveNoWorkingDaysWithoutEndDate() {
        assertThat(newProject().getWorkingDays()).isNull();
    }

    @Test
    void shouldIgnoreMealsWithoutEstimatedTotal() {
        Project project = newProject();
        project.addMeal(newMeal(MealType.LUNCH_AND_DINNER));

        assertThat(project.getTotalPrice()).isEqualByComparingTo("0");
    }

    @Test
    void shouldRecalculateMealTotalsWhenEndDateChanges() {
        Project project = twoWeekProject();
        Meal meal = newMeal(MealType.LUNCH_AND_DINNER);
        project.addMeal(meal);

        project.setEndDate(LocalDate.of(2026, 10, 9));

        assertThat(meal.getWorkingDays()).isEqualTo(5);
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("2130.00");
        assertThat(project.getTotalPrice()).isEqualByComparingTo("2130.00");

        project.setEndDate(null);

        assertThat(meal.getTotalPrice()).isNull();
        assertThat(project.getTotalPrice()).isEqualByComparingTo("0");
    }

    @Test
    void shouldSumAccommodationsInTotalPrice() {
        Accommodation first = new Accommodation(null, "Maria Souza", "11988887777",
                new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"),
                8, false, null, LocalDate.of(2026, 10, 12), new BigDecimal("1000.00"));
        Accommodation second = new Accommodation(null, "Jose Lima", "11988887778",
                new Address(null, "Rua das Flores", "121", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"),
                4, false, null, LocalDate.of(2026, 10, 12), new BigDecimal("500.50"));

        Project project = newProject();
        project.addAccommodation(first);
        project.addAccommodation(second);

        assertThat(project.getAccommodations()).hasSize(2);
        assertThat(project.getTotalPrice()).isEqualByComparingTo("1500.50");
    }

    @Test
    void shouldCompareById() {
        UUID id = UUID.randomUUID();
        Project first = newProject();
        Project second = newProject();

        assertThat(first).isNotEqualTo(second);

        first.setId(id);
        second.setId(id);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldDefaultCompletedToFalse() {
        Project project = newProject();

        project.setCompleted(true);
        assertThat(project.isCompleted()).isTrue();

        project.setCompleted(null);
        assertThat(project.isCompleted()).isFalse();
    }

    @Test
    void shouldValidateOnConstructor() {
        assertThatThrownBy(() -> new Project(null, null, "Obra de reforma", newCostCenter(),
                LocalDate.of(2026, 1, 10), null,
                newClient(), false))
                .isInstanceOf(InvalidProjectException.class);
    }

    private static Project projectBetween(String start, String end) {
        return new Project(null, "OS-2026", null, newCostCenter(), LocalDate.parse(start), LocalDate.parse(end),
                newClient(), false);
    }
}
