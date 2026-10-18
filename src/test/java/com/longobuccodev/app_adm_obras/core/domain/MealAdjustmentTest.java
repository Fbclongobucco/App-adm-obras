package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment.Type;
import com.longobuccodev.app_adm_obras.core.exception.InvalidMealAdjustmentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras intrinsecas do ajuste por data e valor calculado a partir da refeicao. */
class MealAdjustmentTest {

    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 10);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    @Test
    @DisplayName("observacao opcional: sem espacos nas pontas, vazia vira null, ate 100 caracteres")
    void shouldNormalizeNote() {
        assertThat(new MealAdjustment(null, MONDAY, Type.NO_SERVICE, null, null, "  Feriado  ").getNote())
                .isEqualTo("Feriado");
        assertThat(new MealAdjustment(null, MONDAY, Type.NO_SERVICE, null, null, "   ").getNote()).isNull();
        assertThat(new MealAdjustment(null, MONDAY, Type.NO_SERVICE, null, null, "a".repeat(100)).getNote())
                .hasSize(100);
        assertThatThrownBy(() -> new MealAdjustment(null, MONDAY, Type.NO_SERVICE, null, null, "a".repeat(101)))
                .isInstanceOf(InvalidMealAdjustmentException.class)
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.note");
    }

    @Test
    @DisplayName("tipo obrigatorio: meal.adjustment.invalid.type")
    void shouldRequireType() {
        assertThatThrownBy(() -> new MealAdjustment(null, MONDAY, null, null, null, null))
                .isInstanceOf(InvalidMealAdjustmentException.class)
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.type");
    }

    @Test
    @DisplayName("mantem o id informado, gera quando null e informa o dia da semana")
    void shouldKeepIdAndExposeWeekday() {
        UUID id = UUID.randomUUID();

        MealAdjustment adjustment = new MealAdjustment(id, SATURDAY, Type.EXTRA_DAY, 6, 0, null);

        assertThat(adjustment.getId()).isEqualTo(id);
        assertThat(adjustment.getWeekday()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(new MealAdjustment(null, MONDAY, Type.NO_SERVICE, null, null, null).getId()).isNotNull();
    }

    @Test
    @DisplayName("dia extra: marmitas, entregas e refrigerantes so das refeicoes com quantidade maior que zero")
    void shouldPriceExtraDay() {
        Meal meal = MealAdjustmentRulesTest.lunchAndDinner452();

        MealAdjustment lunchOnly = new MealAdjustment(null, SATURDAY, Type.EXTRA_DAY, 6, 0, null);
        MealAdjustment both = new MealAdjustment(null, SATURDAY, Type.EXTRA_DAY, 6, 4, null);

        assertThat(lunchOnly.deliveries(meal)).isEqualTo(1);
        assertThat(lunchOnly.sodas(meal)).isEqualTo(1);
        assertThat(lunchOnly.price(meal)).isEqualByComparingTo("163.00");
        // 6 x 25 + 4 x 22 + 2 x 8 + 2 x 5
        assertThat(both.deliveries(meal)).isEqualTo(2);
        assertThat(both.price(meal)).isEqualByComparingTo("264.00");
    }

    @Test
    @DisplayName("dia extra sem taxa nem refrigerante soma so as marmitas")
    void shouldPriceExtraDayWithoutFeeNorSoda() {
        Meal meal = MealAdjustmentRulesTest.lunchAndDinner452();
        meal.getRestaurant().setLunchDeliveryFee(null);
        meal.getRestaurant().setDinnerDeliveryFee(null);
        meal.getRestaurant().setSodaPrice(null);

        MealAdjustment adjustment = new MealAdjustment(null, SATURDAY, Type.EXTRA_DAY, 6, 0, null);

        assertThat(adjustment.sodas(meal)).isZero();
        assertThat(adjustment.price(meal)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("refeicao que deixou de servir o jantar nao conta a quantidade de jantar do dia extra")
    void shouldIgnoreQuantityOfMealNoLongerServed() {
        Meal meal = MealAdjustmentRulesTest.lunchAndDinner452();
        MealAdjustment adjustment = new MealAdjustment(null, SATURDAY, Type.EXTRA_DAY, 0, 4, null);

        meal.configure(MealType.LUNCH, 10, new BigDecimal("25.00"), null, null);

        assertThat(adjustment.deliveries(meal)).isZero();
        assertThat(adjustment.price(meal)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("sem fornecimento: entregas, refrigerantes e valor zero")
    void noServiceShouldBeWorthZero() {
        Meal meal = MealAdjustmentRulesTest.lunchAndDinner452();

        MealAdjustment adjustment = new MealAdjustment(null, MONDAY, Type.NO_SERVICE, null, null, "Feriado");

        assertThat(adjustment.deliveries(meal)).isZero();
        assertThat(adjustment.sodas(meal)).isZero();
        assertThat(adjustment.price(meal)).isEqualByComparingTo("0");
    }
}
