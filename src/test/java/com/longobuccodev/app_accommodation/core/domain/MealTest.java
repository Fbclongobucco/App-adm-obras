package com.longobuccodev.app_accommodation.core.domain;

import com.longobuccodev.app_accommodation.core.domain.Meal.MealType;
import com.longobuccodev.app_accommodation.core.exception.InvalidMealException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MealTest {

    private static Meal newMeal() {
        return new Meal(null, " Restaurante  do  Ze ", new BigDecimal("25.90"), null, null, null,
                MealType.LUNCH, 4, LocalDate.now());
    }

    @Test
    void shouldNormalizeFieldsAndCalculateTotalPrice() {
        Meal meal = newMeal();

        assertThat(meal.getId()).isNotNull();
        assertThat(meal.getRestaurantName()).isEqualTo("Restaurante do Ze");
        assertThat(meal.getPrice()).isEqualByComparingTo("25.90");
        assertThat(meal.getQuantity()).isEqualTo(4);
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("103.60");
        assertThat(meal.getBilled()).isFalse();
        assertThat(meal.isBilled()).isFalse();
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();

        assertThat(new Meal(id, "Restaurante do Ze", BigDecimal.ONE, false, null, null,
                MealType.LUNCH, 1, LocalDate.now()).getId()).isEqualTo(id);
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
    void shouldRejectBlankRestaurantName() {
        Meal meal = newMeal();

        assertThatThrownBy(() -> meal.setRestaurantName(" "))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.restaurant_name.blank");
    }

    @Test
    void shouldRejectLongRestaurantName() {
        Meal meal = newMeal();

        assertThatThrownBy(() -> meal.setRestaurantName("a".repeat(101)))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.restaurant_name.length");
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
        assertThatThrownBy(() -> new Meal(null, "Restaurante do Ze", BigDecimal.ONE, false, null, null,
                null, 1, LocalDate.now()))
                .isInstanceOf(InvalidMealException.class);
    }
}