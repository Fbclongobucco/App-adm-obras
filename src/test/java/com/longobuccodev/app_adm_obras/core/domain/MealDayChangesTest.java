package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.DayClosing.Status;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment.Type;
import com.longobuccodev.app_adm_obras.core.exception.InvalidMealAdjustmentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Taxa de entrega por refeicao, dia alterado e numeros do dia (change refeicoes-editaveis-por-dia). */
class MealDayChangesTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = LocalDate.of(2026, 10, 16);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 10);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 14);

    private static Project project() {
        return new Project(null, "OS-2026", null, new CostCenter(null, "Centro Norte", "11222333000181"),
                START, END,
                new Client(null, "Cliente Alfa", null, "1133334444",
                        new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000")),
                false);
    }

    /** Entrega do almoco 8.00, do jantar 10.00 e refrigerante 5.00. */
    private static Restaurant restaurant(BigDecimal sodaPrice) {
        return new Restaurant(null, "Cantina da Obra", null, null, project(),
                new BigDecimal("8.00"), new BigDecimal("10.00"), sodaPrice);
    }

    private static Meal meal(Restaurant restaurant, MealType type) {
        return new Meal(null, restaurant, type, false, 10, new BigDecimal("25.00"), 8, new BigDecimal("22.00"));
    }

    /** Almoco 10 x 25.00, jantar 8 x 22.00, entregas 8.00 e 10.00, refrigerante 5.00: 454.00 por dia. */
    private static Meal meal454() {
        return meal(restaurant(new BigDecimal("5.00")), MealType.LUNCH_AND_DINNER);
    }

    private static MealAdjustment changed(Integer lunch, Integer dinner, Integer lunchDeliveries,
                                          Integer dinnerDeliveries, Integer sodas) {
        return new MealAdjustment(null, WEDNESDAY, Type.CHANGED, lunch, dinner, lunchDeliveries, dinnerDeliveries,
                sodas, null);
    }

    @Test
    @DisplayName("taxa de cada refeicao: 454,00 com 8,00 e 10,00; 452,00 com a mesma taxa")
    void deliveryFeePerMeal() {
        assertThat(meal454().getDeliveryDailyPrice()).isEqualByComparingTo("18.00");
        assertThat(meal454().getDailyPrice()).isEqualByComparingTo("454.00");
        Restaurant sameFee = new Restaurant(null, "Cantina", null, null, project(), new BigDecimal("8.00"),
                new BigDecimal("5.00"));
        assertThat(meal(sameFee, MealType.LUNCH_AND_DINNER).getDailyPrice()).isEqualByComparingTo("452.00");
    }

    @Test
    @DisplayName("refeicao Jantar usa a taxa do jantar; vazia conta zero")
    void dinnerWithoutDinnerFee() {
        Restaurant restaurant = new Restaurant(null, "Cantina", null, null, project(), new BigDecimal("8.00"),
                null, null);
        assertThat(meal(restaurant, MealType.DINNER).getDeliveryDailyPrice()).isEqualByComparingTo("0");
        assertThat(meal(restaurant, MealType.LUNCH).getDeliveryDailyPrice()).isEqualByComparingTo("8.00");
    }

    @Test
    @DisplayName("dia alterado com duas entregas no almoco: 3 entregas, 3 refrigerantes, 489,00")
    void changedDayWithDeliveries() {
        Meal meal = meal454();

        MealAdjustment saved = meal.putAdjustment(changed(10, 9, 2, 1, null));

        assertThat(saved.lunchDeliveries(meal)).isEqualTo(2);
        assertThat(saved.dinnerDeliveries(meal)).isEqualTo(1);
        assertThat(saved.sodas(meal)).isEqualTo(3);
        assertThat(saved.price(meal)).isEqualByComparingTo("489.00");
        DayClosing day = meal.dayClosing(WEDNESDAY);
        assertThat(day.status()).isEqualTo(Status.CHANGED);
        assertThat(day.deliveries()).isEqualTo(3);
        assertThat(day.price()).isEqualByComparingTo("489.00");
    }

    @Test
    @DisplayName("refrigerantes informados: 484,00; uma janta a menos no padrao: 432,00")
    void changedDayNumbers() {
        Meal meal = meal454();
        assertThat(meal.putAdjustment(changed(10, 9, 2, 1, 2)).price(meal)).isEqualByComparingTo("484.00");
        MealAdjustment fewer = meal.putAdjustment(changed(10, 7, null, null, null));
        assertThat(fewer.deliveries(meal)).isEqualTo(2);
        assertThat(fewer.sodas(meal)).isEqualTo(2);
        assertThat(fewer.price(meal)).isEqualByComparingTo("432.00");
        assertThat(meal.getAdjustments()).hasSize(1);
    }

    @Test
    @DisplayName("semana de 12/10 com dia alterado vale 2305,00 e o total 4575,00")
    void weekAndTotal() {
        Meal meal = meal454();
        meal.putAdjustment(changed(10, 9, 2, 1, null));
        List<ClosingWeek> weeks = ClosingWeek.of(START, END, null);

        WeekClosing week = WeekClosing.of(ClosingWeek.select(weeks, WEDNESDAY, null), List.of(meal), START, END);

        assertThat(week.totals().serviceDays()).isEqualTo(5);
        assertThat(week.totals().price()).isEqualByComparingTo("2305.00");
        assertThat(meal.getChangedDays()).isEqualTo(1);
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("4575.00");
    }

    @Test
    @DisplayName("regravar o dia extra mantem o id e a criacao: 163,00 -> 266,00")
    void putReplacesKeepingId() {
        Meal meal = meal454();
        MealAdjustment first = meal.putAdjustment(new MealAdjustment(null, SATURDAY, Type.EXTRA_DAY, 6, 0, null));
        first.markCreated(Instant.parse("2026-10-01T10:00:00Z"));
        assertThat(first.price(meal)).isEqualByComparingTo("163.00");

        MealAdjustment second = meal.putAdjustment(new MealAdjustment(null, SATURDAY, Type.EXTRA_DAY, 6, 4, null));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getCreatedAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
        assertThat(second.price(meal)).isEqualByComparingTo("266.00");
        assertThat(meal.getAdjustments()).singleElement().isSameAs(second);
    }

    @Test
    @DisplayName("regras do dia: fim de semana, zerado, entrega sem marmita, limites e refrigerante")
    void rules() {
        Meal meal = meal454();
        assertError(() -> meal.putAdjustment(new MealAdjustment(null, SATURDAY, Type.CHANGED, 6, 0, null)),
                "meal.adjustment.invalid.changed_day");
        assertError(() -> meal.putAdjustment(changed(0, 0, null, null, null)), "meal.adjustment.invalid.quantity");
        assertError(() -> meal.putAdjustment(changed(10, 0, null, 1, null)),
                "meal.adjustment.invalid.delivery_without_meal");
        assertError(() -> meal.putAdjustment(changed(10, 8, 101, null, null)), "meal.adjustment.invalid.deliveries");
        assertError(() -> meal.putAdjustment(changed(10, 8, null, null, -1)), "meal.adjustment.invalid.sodas");
        Meal withoutSoda = meal(restaurant(null), MealType.LUNCH_AND_DINNER);
        assertError(() -> withoutSoda.putAdjustment(changed(10, 8, null, null, 2)),
                "meal.adjustment.invalid.soda_not_supplied");
        assertThat(withoutSoda.putAdjustment(changed(10, 8, null, null, 0)).sodas(withoutSoda)).isZero();
        assertThat(meal.getAdjustments()).isEmpty();
    }

    @Test
    @DisplayName("restaurante: jantar sem fornecimento no dia e almoco no padrao")
    void restaurantDinnerSuspended() {
        Restaurant restaurant = restaurant(new BigDecimal("5.00"));
        Meal lunch = meal(restaurant, MealType.LUNCH);
        Meal dinner = meal(restaurant, MealType.DINNER);
        dinner.putAdjustment(new MealAdjustment(null, WEDNESDAY, Type.NO_SERVICE, null, null, null));
        List<ClosingWeek> weeks = ClosingWeek.of(START, END, null);

        DayClosing day = WeekClosing.of(ClosingWeek.select(weeks, WEDNESDAY, null), List.of(lunch, dinner), START,
                END).days().get(2);

        assertThat(day.status()).isEqualTo(Status.SERVICE);
        assertThat(day.partialNoService()).isTrue();
        assertThat(day.lunchMeals()).isEqualTo(10);
        assertThat(day.dinnerMeals()).isZero();
        assertThat(day.deliveries()).isEqualTo(1);
        assertThat(day.meals()).extracting(DayClosing::mealId).containsExactly(lunch.getId(), dinner.getId());
        assertThat(lunch.isDefaultDay(new MealAdjustment(null, WEDNESDAY, Type.CHANGED, 10, null, null))).isTrue();
        assertThat(lunch.isDefaultDay(new MealAdjustment(null, WEDNESDAY, Type.CHANGED, 9, null, null))).isFalse();
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(InvalidMealAdjustmentException.class)
                .extracting("errorCode").isEqualTo(code);
    }
}
