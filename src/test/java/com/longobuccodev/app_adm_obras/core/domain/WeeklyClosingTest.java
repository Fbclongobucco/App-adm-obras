package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.DayClosing.Status;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Semanas de fechamento e fechamento diario e semanal (change fechamento-semanal-refeicoes). */
class WeeklyClosingTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = LocalDate.of(2026, 10, 16);

    private static LocalDate day(int dayOfOctober) {
        return LocalDate.of(2026, 10, dayOfOctober);
    }

    private static Project project(LocalDate start, LocalDate end) {
        return new Project(null, "OS-2026", null, new CostCenter(null, "Centro Norte", "11222333000181"),
                start, end,
                new Client(null, "Cliente Alfa", null, "1133334444",
                        new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000")),
                false);
    }

    /** Restaurante com taxa 8.00 e refrigerante 5.00. */
    private static Restaurant restaurant(Project project) {
        return new Restaurant(null, "Cantina da Obra", null, null, project,
                new BigDecimal("8.00"), new BigDecimal("5.00"));
    }

    private static Meal meal(Restaurant restaurant, MealType type) {
        return new Meal(null, restaurant, type, false, 10, new BigDecimal("25.00"), 8, new BigDecimal("22.00"));
    }

    private static ClosingWeek week(List<ClosingWeek> weeks, int dayOfOctober) {
        return ClosingWeek.select(weeks, day(dayOfOctober), null);
    }

    @Test
    @DisplayName("obra de segunda a domingo: 2 semanas completas")
    void fullWeeks() {
        List<ClosingWeek> weeks = ClosingWeek.of(day(5), day(18), day(14));

        assertThat(weeks).containsExactly(
                new ClosingWeek(day(5), day(11), false, false),
                new ClosingWeek(day(12), day(18), false, true));
    }

    @Test
    @DisplayName("inicio e termino no meio da semana: semanas cortadas e parciais")
    void partialWeeks() {
        List<ClosingWeek> weeks = ClosingWeek.of(day(7), day(16), day(1));

        assertThat(weeks).containsExactly(
                new ClosingWeek(day(7), day(11), true, false),
                new ClosingWeek(day(12), day(16), true, false));
        assertThat(weeks.getFirst().days()).first().isEqualTo(day(5));
        assertThat(weeks.getFirst().days()).hasSize(7).last().isEqualTo(day(11));
    }

    @Test
    @DisplayName("obra sem termino: semanas ate a semana de hoje; antes do inicio, so a semana do inicio")
    void projectWithoutEnd() {
        List<ClosingWeek> weeks = ClosingWeek.of(day(5), null, day(21));

        assertThat(weeks).extracting(ClosingWeek::start).containsExactly(day(5), day(12), day(19));
        assertThat(weeks.getLast()).isEqualTo(new ClosingWeek(day(19), day(25), false, true));
        assertThat(ClosingWeek.of(day(14), null, day(1))).extracting(ClosingWeek::start).containsExactly(day(14));
    }

    @Test
    @DisplayName("semana padrao: atual, primeira antes do inicio, ultima depois do termino; selecao por data")
    void defaultAndSelectedWeek() {
        LocalDate end = day(30);
        assertThat(ClosingWeek.defaultOf(ClosingWeek.of(START, end, day(14)), day(14)).start()).isEqualTo(day(12));
        assertThat(ClosingWeek.defaultOf(ClosingWeek.of(START, end, day(1)), day(1)).start()).isEqualTo(day(5));
        LocalDate november = LocalDate.of(2026, 11, 10);
        assertThat(ClosingWeek.defaultOf(ClosingWeek.of(START, END, november), november).start()).isEqualTo(day(12));

        List<ClosingWeek> weeks = ClosingWeek.of(START, end, day(14));
        assertThat(ClosingWeek.select(weeks, day(28), day(14)).start()).isEqualTo(day(26));
        assertThat(ClosingWeek.select(weeks, LocalDate.of(2027, 1, 1), day(14)).start()).isEqualTo(day(12));
        assertThat(ClosingWeek.select(weeks, null, day(14)).start()).isEqualTo(day(12));
    }

    @Test
    @DisplayName("cenario da spec: 2.423,00 + 1.808,00 = 4.231,00 = total estimado")
    void weeksSumToTotal() {
        Project project = project(START, END);
        Meal meal = meal(restaurant(project), MealType.LUNCH_AND_DINNER);
        meal.addAdjustment(new MealAdjustment(null, day(10), Type.EXTRA_DAY, 6, 0, null));
        MealAdjustment holiday = meal.addAdjustment(new MealAdjustment(null, day(12), Type.NO_SERVICE, null, null, null));
        List<ClosingWeek> weeks = ClosingWeek.of(START, END, day(14));

        WeekClosing first = WeekClosing.of(weeks.get(0), List.of(meal), START, END);
        WeekClosing second = WeekClosing.of(weeks.get(1), List.of(meal), START, END);

        assertThat(first.days()).extracting(DayClosing::status).containsExactly(
                Status.SERVICE, Status.SERVICE, Status.SERVICE, Status.SERVICE, Status.SERVICE,
                Status.EXTRA_DAY, Status.OFF);
        DayClosing saturday = first.days().get(5);
        assertThat(saturday.lunchMeals()).isEqualTo(6);
        assertThat(saturday.dinnerMeals()).isZero();
        assertThat(saturday.deliveries()).isEqualTo(1);
        assertThat(saturday.price()).isEqualByComparingTo("163.00");
        assertThat(first.totals().serviceDays()).isEqualTo(6);
        assertThat(first.totals().lunchMeals()).isEqualTo(56);
        assertThat(first.totals().price()).isEqualByComparingTo("2423.00");

        assertThat(second.days()).extracting(DayClosing::status).containsExactly(
                Status.NO_SERVICE, Status.SERVICE, Status.SERVICE, Status.SERVICE, Status.SERVICE,
                Status.OUT_OF_PERIOD, Status.OUT_OF_PERIOD);
        assertThat(second.days().getFirst().adjustmentIds()).containsExactly(holiday.getId());
        assertThat(second.totals().serviceDays()).isEqualTo(4);
        assertThat(second.totals().price()).isEqualByComparingTo("1808.00");

        assertThat(first.totals().price().add(second.totals().price())).isEqualByComparingTo(meal.getTotalPrice());
    }

    @Test
    @DisplayName("restaurante com Almoco e Jantar separados: 452,00 por dia e 2.260,00 na semana")
    void restaurantSumsMeals() {
        Project project = project(START, END);
        Restaurant restaurant = restaurant(project);
        Meal lunch = meal(restaurant, MealType.LUNCH);
        Meal dinner = meal(restaurant, MealType.DINNER);

        WeekClosing closing = WeekClosing.of(week(ClosingWeek.of(START, END, null), 5), List.of(lunch, dinner),
                START, END);

        DayClosing monday = closing.days().getFirst();
        assertThat(monday.status()).isEqualTo(Status.SERVICE);
        assertThat(monday.lunchMeals()).isEqualTo(10);
        assertThat(monday.dinnerMeals()).isEqualTo(8);
        assertThat(monday.deliveries()).isEqualTo(2);
        assertThat(monday.sodas()).isEqualTo(2);
        assertThat(monday.price()).isEqualByComparingTo("452.00");
        assertThat(closing.totals().price()).isEqualByComparingTo("2260.00");
    }

    @Test
    @DisplayName("restaurante: feriado em todas e sem fornecimento; em parte e parcial; dia extra em uma")
    void restaurantStatuses() {
        Project project = project(START, END);
        Restaurant restaurant = restaurant(project);
        Meal lunch = meal(restaurant, MealType.LUNCH);
        Meal dinner = meal(restaurant, MealType.DINNER);
        lunch.addAdjustment(new MealAdjustment(null, day(12), Type.NO_SERVICE, null, null, null));
        dinner.addAdjustment(new MealAdjustment(null, day(12), Type.NO_SERVICE, null, null, null));
        lunch.addAdjustment(new MealAdjustment(null, day(13), Type.NO_SERVICE, null, null, null));
        lunch.addAdjustment(new MealAdjustment(null, day(10), Type.EXTRA_DAY, 6, null, null));
        List<ClosingWeek> weeks = ClosingWeek.of(START, END, null);

        WeekClosing first = WeekClosing.of(week(weeks, 5), List.of(lunch, dinner), START, END);
        WeekClosing second = WeekClosing.of(week(weeks, 12), List.of(lunch, dinner), START, END);

        assertThat(first.days().get(5).status()).isEqualTo(Status.EXTRA_DAY);
        assertThat(first.days().get(5).price()).isEqualByComparingTo("163.00");
        DayClosing holiday = second.days().get(0);
        assertThat(holiday.status()).isEqualTo(Status.NO_SERVICE);
        assertThat(holiday.adjustmentIds()).hasSize(2);
        assertThat(holiday.price()).isEqualByComparingTo("0");
        DayClosing partial = second.days().get(1);
        assertThat(partial.status()).isEqualTo(Status.SERVICE);
        assertThat(partial.partialNoService()).isTrue();
        assertThat(partial.price()).isEqualByComparingTo("189.00");
    }

    @Test
    @DisplayName("sem refeicoes: situacao do calendario e valores zerados")
    void withoutMeals() {
        WeekClosing closing = WeekClosing.of(week(ClosingWeek.of(day(7), END, null), 7), List.of(), day(7), END);

        assertThat(closing.days()).extracting(DayClosing::status).containsExactly(
                Status.OUT_OF_PERIOD, Status.OUT_OF_PERIOD, Status.SERVICE, Status.SERVICE, Status.SERVICE,
                Status.OFF, Status.OFF);
        assertThat(closing.totals().price()).isEqualByComparingTo("0");
    }
}
