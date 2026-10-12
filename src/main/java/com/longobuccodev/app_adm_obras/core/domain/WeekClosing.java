package com.longobuccodev.app_adm_obras.core.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fechamento de uma semana (segunda a domingo) de uma ou mais refeicoes, dia a dia e com totais.
 * Varias refeicoes sao somadas por dia, como no fechamento do restaurante.
 */
public record WeekClosing(ClosingWeek week, List<DayClosing> days, Totals totals) {

    public record Totals(int serviceDays, int lunchMeals, int dinnerMeals, int deliveries, int sodas,
                         BigDecimal price) {
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
        Totals totals = new Totals(serviceDays,
                days.stream().mapToInt(DayClosing::lunchMeals).sum(),
                days.stream().mapToInt(DayClosing::dinnerMeals).sum(),
                days.stream().mapToInt(DayClosing::deliveries).sum(),
                days.stream().mapToInt(DayClosing::sodas).sum(),
                days.stream().map(DayClosing::price).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new WeekClosing(week, List.copyOf(days), totals);
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
                perMeal.stream().mapToInt(DayClosing::lunchMeals).sum(),
                perMeal.stream().mapToInt(DayClosing::dinnerMeals).sum(),
                perMeal.stream().mapToInt(DayClosing::deliveries).sum(),
                perMeal.stream().mapToInt(DayClosing::sodas).sum(),
                perMeal.stream().map(DayClosing::price).reduce(BigDecimal.ZERO, BigDecimal::add),
                adjustmentIds);
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
}
