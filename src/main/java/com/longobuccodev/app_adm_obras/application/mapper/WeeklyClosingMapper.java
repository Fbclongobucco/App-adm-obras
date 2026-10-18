package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.closing.ClosingWeekDTO;
import com.longobuccodev.app_adm_obras.application.dto.closing.DayClosingDTO;
import com.longobuccodev.app_adm_obras.application.dto.closing.WeeklyClosingResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.ClosingWeek;
import com.longobuccodev.app_adm_obras.core.domain.DayClosing;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.WeekClosing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class WeeklyClosingMapper {

    private WeeklyClosingMapper() {
    }

    /** Semanas da obra e fechamento da semana pedida (ou da padrao) para as refeicoes informadas. */
    public static WeeklyClosingResponseDTO toResponse(Project project, List<Meal> meals, LocalDate requested,
                                                      LocalDate today) {
        List<ClosingWeek> weeks = ClosingWeek.of(project.getStartDate(), project.getEndDate(), today);
        ClosingWeek selected = ClosingWeek.select(weeks, requested, today);
        List<ClosingWeekDTO> weekDtos = weeks.stream()
                .map(week -> new ClosingWeekDTO(week.start(), week.end(), week.partial(), week.current()))
                .toList();
        if (selected == null) {
            return new WeeklyClosingResponseDTO(weekDtos, null, List.of(),
                    new WeeklyClosingResponseDTO.Totals(0, 0, 0, 0, 0, BigDecimal.ZERO));
        }
        WeekClosing closing = WeekClosing.of(selected, meals, project.getStartDate(), project.getEndDate());
        WeekClosing.Totals totals = closing.totals();
        return new WeeklyClosingResponseDTO(weekDtos, selected.start(),
                closing.days().stream().map(WeeklyClosingMapper::toDto).toList(),
                new WeeklyClosingResponseDTO.Totals(totals.serviceDays(), totals.lunchMeals(), totals.dinnerMeals(),
                        totals.deliveries(), totals.sodas(), totals.price()));
    }

    private static DayClosingDTO toDto(DayClosing day) {
        return new DayClosingDTO(day.date(), day.date().getDayOfWeek(), day.status(), day.partialNoService(),
                day.lunchMeals(), day.dinnerMeals(), day.lunchDeliveries(), day.dinnerDeliveries(),
                day.deliveries(), day.sodas(), day.price(), day.adjustmentIds(), day.note(),
                day.meals().stream().map(meal -> new DayClosingDTO.MealDay(meal.mealId(), meal.status(),
                        meal.lunchMeals(), meal.dinnerMeals(), meal.lunchDeliveries(), meal.dinnerDeliveries(),
                        meal.sodas(), meal.price())).toList());
    }
}
