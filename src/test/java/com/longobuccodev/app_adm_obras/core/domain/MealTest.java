package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.exception.InvalidMealException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MealTest {

    private static Meal newMeal() {
        return new Meal(null, restaurant(), new BigDecimal("25.90"), null, MealType.LUNCH, 4, LocalDate.now());
    }

    @Test
    void shouldNormalizeFieldsAndCalculateTotalPrice() {
        Meal meal = newMeal();

        assertThat(meal.getId()).isNotNull();
        assertThat(meal.getRestaurant().getName()).isEqualTo("Restaurante do Ze");
        assertThat(meal.getPrice()).isEqualByComparingTo("25.90");
        assertThat(meal.getQuantity()).isEqualTo(4);
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("103.60");
        assertThat(meal.getBilled()).isFalse();
        assertThat(meal.isBilled()).isFalse();
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();

        assertThat(new Meal(id, restaurant(), BigDecimal.ONE, false, MealType.LUNCH, 1, LocalDate.now()).getId())
                .isEqualTo(id);
        assertThat(newMeal().getId()).isNotNull();
    }

    @Test
    void shouldRecalculateTotalPriceWhenPriceChanges() {
        Meal meal = newMeal();

        meal.setPrice(new BigDecimal("30.00"));

        assertThat(meal.getTotalPrice()).isEqualByComparingTo("120.00");
    }

    @Test
    void shouldRecalculateTotalPriceWhenQuantityChanges() {
        Meal meal = newMeal();

        meal.setQuantity(10);

        assertThat(meal.getTotalPrice()).isEqualByComparingTo("259.00");
    }

    @Test
    void shouldCompareById() {
        UUID id = UUID.randomUUID();
        Meal first = newMeal();
        Meal second = newMeal();

        assertThat(first).isNotEqualTo(second);

        first.setId(id);
        second.setId(id);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldRequireRestaurant() {
        Meal meal = newMeal();

        assertThatThrownBy(() -> meal.setRestaurant(null))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.restaurant");
    }

    @Test
    void shouldTakeProjectFromRestaurant() {
        Restaurant restaurant = restaurant();

        Meal meal = new Meal(null, restaurant, BigDecimal.ONE, false, MealType.DINNER, 1, LocalDate.now());

        assertThat(meal.getProject()).isSameAs(restaurant.getProject());
    }

    @Test
    void shouldRejectZeroOrNegativePrice() {
        Meal meal = newMeal();

        assertThatThrownBy(() -> meal.setPrice(BigDecimal.ZERO))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.price");
        assertThatThrownBy(() -> meal.setPrice(new BigDecimal("-10.00")))
                .isInstanceOf(InvalidMealException.class);
        assertThatThrownBy(() -> meal.setPrice(null))
                .isInstanceOf(InvalidMealException.class);
    }

    @Test
    void shouldRejectMissingMealType() {
        Meal meal = newMeal();

        assertThatThrownBy(() -> meal.setMealType(null))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.type");
    }

    @Test
    void shouldRejectInvalidQuantity() {
        Meal meal = newMeal();

        assertThatThrownBy(() -> meal.setQuantity(0))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.quantity");
        assertThatThrownBy(() -> meal.setQuantity(10001))
                .isInstanceOf(InvalidMealException.class);
    }

    @Test
    void shouldRejectMissingAndFutureDate() {
        Meal meal = newMeal();

        assertThatThrownBy(() -> meal.setDate(null))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.date");
        assertThatThrownBy(() -> meal.setDate(LocalDate.now().plusDays(1)))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.date.future");
    }

    @Test
    void shouldDefaultBilledToFalse() {
        Meal meal = newMeal();

        meal.setBilled(true);
        assertThat(meal.isBilled()).isTrue();

        meal.setBilled(null);
        assertThat(meal.isBilled()).isFalse();
        assertThat(meal.getBilled()).isFalse();
    }

    @Test
    void shouldValidateOnConstructor() {
        assertThatThrownBy(() -> new Meal(null, restaurant(), BigDecimal.ONE, false, null, 1, LocalDate.now()))
                .isInstanceOf(InvalidMealException.class);
    }

    private static Restaurant restaurant() {
        Project project = new Project(null, "OS-100", null, new CostCenter(null, "Centro Norte", "11222333000181"),
                LocalDate.of(2026, 1, 5), null,
                new Client(null, "Cliente Alfa", "alfa@exemplo.test", "1133334444",
                        new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000")),
                false);
        return new Restaurant(null, " Restaurante  do  Ze ", null, null, project);
    }
}
