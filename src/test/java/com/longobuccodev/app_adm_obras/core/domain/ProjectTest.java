package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.exception.InvalidProjectException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectTest {

    private static Client newClient() {
        return new Client(null, "Construtora Alfa", "contato@alfa.com", "1133334444",
                new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"),
                null);
    }

    private static CostCenter newCostCenter() {
        return new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81");
    }

    private static Meal newMeal(MealType mealType, String price, int quantity) {
        return new Meal(null, "Restaurante do Ze", new BigDecimal(price), false, null, null,
                mealType, quantity, LocalDate.now());
    }

    private static Project newProject() {
        return new Project(null, " os-1234 ", "Obra de reforma ", newCostCenter(),
                LocalDate.of(2026, 1, 10), null,
                newClient(), null, null, null);
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
                newClient(), null, null, false).getId()).isEqualTo(id);
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
    void shouldRouteLunchAndDinnerByMealType() {
        Project project = newProject();
        Meal lunch = newMeal(MealType.LUNCH, "25.00", 10);
        Meal dinner = newMeal(MealType.DINNER, "30.00", 10);

        project.addMeal(lunch);
        project.addMeal(dinner);

        assertThat(project.getLunch()).isSameAs(lunch);
        assertThat(project.getDinner()).isSameAs(dinner);
        assertThat(project.getTotalPrice()).isEqualByComparingTo("550.00");
    }

    @Test
    void shouldRejectDuplicatedMealType() {
        Project project = newProject();
        project.addMeal(newMeal(MealType.LUNCH, "25.00", 1));

        assertThatThrownBy(() -> project.addMeal(newMeal(MealType.LUNCH, "25.00", 1)))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.duplicated_meal_type");
    }

    @Test
    void shouldRejectMealOnWrongProperty() {
        Project project = newProject();

        assertThatThrownBy(() -> project.setLunch(newMeal(MealType.DINNER, "30.00", 1)))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.meal_type");
    }

    @Test
    void shouldRejectNullMeal() {
        Project project = newProject();

        assertThatThrownBy(() -> project.addMeal(null))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.meal");
    }

    @Test
    void shouldRecalculateTotalPriceWhenMealIsRemoved() {
        Project project = newProject();
        project.addMeal(newMeal(MealType.LUNCH, "25.00", 10));
        project.addMeal(newMeal(MealType.DINNER, "30.00", 10));

        project.removeMeal(MealType.DINNER);

        assertThat(project.getDinner()).isNull();
        assertThat(project.getTotalPrice()).isEqualByComparingTo("250.00");
    }

    @Test
    void shouldRejectRemovingMealThatDoesNotExist() {
        Project project = newProject();

        assertThatThrownBy(() -> project.removeMeal(MealType.LUNCH))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.missing_meal_type");
    }

    @Test
    void shouldSumAccommodationsInTotalPrice() {
        Set<Accommodation> accommodations = new HashSet<>();
        accommodations.add(new Accommodation(null, "Maria Souza", "11988887777",
                new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"),
                8, 30, false, null, null, new BigDecimal("1000.00")));
        accommodations.add(new Accommodation(null, "Jose Lima", "11988887778",
                new Address(null, "Rua das Flores", "121", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"),
                4, 15, false, null, null, new BigDecimal("500.50")));

        Project project = new Project(null, " os-1234 ", "Obra de reforma ", newCostCenter(),
                LocalDate.of(2026, 1, 10), null,
                newClient(), accommodations, null, null);
        accommodations.clear();

        assertThat(project.getAccommodations()).hasSize(2);
        assertThat(project.getTotalPrice()).isEqualByComparingTo("1500.50");
    }

    @Test
    void shouldRecalculateTotalPriceWhenMealPriceChanges() {
        Project project = newProject();
        Meal lunch = newMeal(MealType.LUNCH, "25.00", 10);
        project.addMeal(lunch);

        lunch.setPrice(new BigDecimal("30.00"));
        project.setLunch(lunch);

        assertThat(project.getTotalPrice()).isEqualByComparingTo("300.00");
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
                newClient(), null, null, false))
                .isInstanceOf(InvalidProjectException.class);
    }
}