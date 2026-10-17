package com.longobuccodev.app_adm_obras.core.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Fechamento de uma semana (segunda a domingo) de uma ou mais refeicoes, dia a dia e com totais.
 * Varias refeicoes sao somadas por dia, como no fechamento do restaurante.
 */
public final class WeekClosing {

    public static final class Totals {

        private final int serviceDays;
        private final int lunchMeals;
        private final int dinnerMeals;
        private final int deliveries;
        private final int sodas;
        private final BigDecimal price;

        public Totals(int serviceDays, int lunchMeals, int dinnerMeals, int deliveries, int sodas,
                      BigDecimal price) {
            this.serviceDays = serviceDays;
            this.lunchMeals = lunchMeals;
            this.dinnerMeals = dinnerMeals;
            this.deliveries = deliveries;
            this.sodas = sodas;
            this.price = price;
        }

        public int serviceDays() {
            return serviceDays;
        }

        public int lunchMeals() {
            return lunchMeals;
        }

        public int dinnerMeals() {
            return dinnerMeals;
        }

        public int deliveries() {
            return deliveries;
        }

        public int sodas() {
            return sodas;
        }

        public BigDecimal price() {
            return price;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Totals other
                    && serviceDays == other.serviceDays
                    && lunchMeals == other.lunchMeals
                    && dinnerMeals == other.dinnerMeals
                    && deliveries == other.deliveries
                    && sodas == other.sodas
                    && Objects.equals(price, other.price);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceDays, lunchMeals, dinnerMeals, deliveries, sodas, price);
        }

        @Override
        public String toString() {
            return "Totals[serviceDays=" + serviceDays + ", lunchMeals=" + lunchMeals + ", dinnerMeals="
                    + dinnerMeals + ", deliveries=" + deliveries + ", sodas=" + sodas + ", price=" + price + "]";
        }
    }

    private final ClosingWeek week;
    private final List<DayClosing> days;
    private final Totals totals;

    public WeekClosing(ClosingWeek week, List<DayClosing> days, Totals totals) {
        this.week = week;
        this.days = List.copyOf(days);
        this.totals = totals;
    }

    public ClosingWeek week() {
        return week;
    }

    public List<DayClosing> days() {
        return days;
    }

    public Totals totals() {
        return totals;
    }

    /**
     * @param projectStart e projectEnd definem os dias fora do periodo quando nao ha refeicoes
     */
    public static WeekClosing of(ClosingWeek week, List<Meal> meals, LocalDate projectStart, LocalDate projectEnd) {
        List<DayClosing> days = new ArrayList<>();
        for (LocalDate date : week.days()) {
            List<DayClosing> perMeal = meals.stream().map(meal -> meal.dayClosing(date)).toList();
            days.add(perMeal.isEmpty() ? calendarDay(date, projectStart, projectEnd) : sum(date, perMeal));
        }
        int serviceDays = (int) days.stream().filter(DayClosing::isServiceDay).count();
        Totals totals = new Totals(serviceDays, sumInt(days, DayClosing::lunchMeals),
                sumInt(days, DayClosing::dinnerMeals), sumInt(days, DayClosing::deliveries),
                sumInt(days, DayClosing::sodas), sumPrice(days));
        return new WeekClosing(week, days, totals);
    }

    /**
     * Situacao somada: dia extra se alguma refeicao tem; sem fornecimento se todas estao sem; fornecimento
     * se alguma fornece (parcial quando outra esta sem fornecimento); senao fim de semana ou fora do periodo.
     */
    private static DayClosing sum(LocalDate date, List<DayClosing> perMeal) {
        boolean anyExtra = has(perMeal, DayClosing.Status.EXTRA_DAY);
        boolean anyNoService = has(perMeal, DayClosing.Status.NO_SERVICE);
        boolean allNoService = perMeal.stream().allMatch(day -> day.status() == DayClosing.Status.NO_SERVICE);
        DayClosing.Status status;
        if (anyExtra) {
            status = DayClosing.Status.EXTRA_DAY;
        } else if (allNoService) {
            status = DayClosing.Status.NO_SERVICE;
        } else if (has(perMeal, DayClosing.Status.SERVICE)) {
            status = DayClosing.Status.SERVICE;
        } else if (has(perMeal, DayClosing.Status.OFF)) {
            status = DayClosing.Status.OFF;
        } else {
            status = DayClosing.Status.OUT_OF_PERIOD;
        }
        List<UUID> adjustmentIds = perMeal.stream().flatMap(day -> day.adjustmentIds().stream()).toList();
        return new DayClosing(date, status, status == DayClosing.Status.SERVICE && anyNoService,
                sumInt(perMeal, DayClosing::lunchMeals), sumInt(perMeal, DayClosing::dinnerMeals),
                sumInt(perMeal, DayClosing::deliveries), sumInt(perMeal, DayClosing::sodas), sumPrice(perMeal),
                adjustmentIds);
    }

    private static int sumInt(List<DayClosing> days, ToIntFunction<DayClosing> field) {
        return days.stream().mapToInt(field).sum();
    }

    private static BigDecimal sumPrice(List<DayClosing> days) {
        return days.stream().map(DayClosing::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean has(List<DayClosing> days, DayClosing.Status status) {
        return days.stream().anyMatch(day -> day.status() == status);
    }

    /** Sem refeicoes: so a situacao do calendario, com numeros zerados. */
    private static DayClosing calendarDay(LocalDate date, LocalDate projectStart, LocalDate projectEnd) {
        boolean inPeriod = projectStart != null && !date.isBefore(projectStart)
                && (projectEnd == null || !date.isAfter(projectEnd));
        if (!inPeriod) {
            return DayClosing.empty(date, DayClosing.Status.OUT_OF_PERIOD);
        }
        DayOfWeek weekday = date.getDayOfWeek();
        boolean weekend = weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY;
        return DayClosing.empty(date, weekend ? DayClosing.Status.OFF : DayClosing.Status.SERVICE);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WeekClosing other
                && Objects.equals(week, other.week)
                && days.equals(other.days)
                && Objects.equals(totals, other.totals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(week, days, totals);
    }

    @Override
    public String toString() {
        return "WeekClosing[week=" + week + ", days=" + days + ", totals=" + totals + "]";
    }
}
