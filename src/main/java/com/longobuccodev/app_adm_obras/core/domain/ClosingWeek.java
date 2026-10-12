package com.longobuccodev.app_adm_obras.core.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Semana de fechamento das refeicoes, de segunda a domingo, cortada no periodo da obra.
 *
 * @param start   primeiro dia da semana dentro da obra
 * @param end     ultimo dia da semana dentro da obra
 * @param partial a semana foi cortada pelo inicio ou pelo termino da obra
 * @param current a semana contem a data de hoje
 */
public record ClosingWeek(LocalDate start, LocalDate end, boolean partial, boolean current) {

    public LocalDate monday() {
        return monday(start);
    }

    /** Os 7 dias da semana, de segunda a domingo, inclusive os fora da obra. */
    public List<LocalDate> days() {
        LocalDate monday = monday();
        return monday.datesUntil(monday.plusDays(7)).toList();
    }

    public boolean contains(LocalDate date) {
        LocalDate monday = monday();
        return date != null && !date.isBefore(monday) && date.isBefore(monday.plusDays(7));
    }

    /**
     * Semanas com algum dia entre o inicio e o termino da obra, em ordem. Sem termino, ate a semana de hoje,
     * e nunca antes da semana do inicio.
     */
    public static List<ClosingWeek> of(LocalDate projectStart, LocalDate projectEnd, LocalDate today) {
        if (projectStart == null) {
            return List.of();
        }
        LocalDate last = projectEnd != null ? projectEnd : max(today, projectStart);
        List<ClosingWeek> weeks = new ArrayList<>();
        for (LocalDate monday = monday(projectStart); !monday.isAfter(last); monday = monday.plusDays(7)) {
            LocalDate sunday = monday.plusDays(6);
            LocalDate start = max(monday, projectStart);
            LocalDate end = projectEnd == null ? sunday : min(sunday, projectEnd);
            boolean current = today != null && !today.isBefore(monday) && !today.isAfter(sunday);
            weeks.add(new ClosingWeek(start, end, !start.equals(monday) || !end.equals(sunday), current));
        }
        return weeks;
    }

    /** Semana atual se estiver na lista; antes da primeira, a primeira; depois, a ultima. */
    public static ClosingWeek defaultOf(List<ClosingWeek> weeks, LocalDate today) {
        if (weeks.isEmpty()) {
            return null;
        }
        return weeks.stream().filter(ClosingWeek::current).findFirst()
                .orElseGet(() -> today != null && today.isBefore(weeks.getFirst().monday())
                        ? weeks.getFirst() : weeks.getLast());
    }

    /** Semana que contem a data pedida; ausente ou fora da lista, a semana padrao. */
    public static ClosingWeek select(List<ClosingWeek> weeks, LocalDate requested, LocalDate today) {
        return weeks.stream().filter(week -> week.contains(requested)).findFirst()
                .orElseGet(() -> defaultOf(weeks, today));
    }

    private static LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return b == null || a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
