package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealProjectionDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;
import com.longobuccodev.app_adm_obras.core.domain.Meal.BillingPeriod;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.client;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenter;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.mealRequest;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.restaurant;
import static org.assertj.core.api.Assertions.assertThat;

class MealMapperTest {

    @Test
    void shouldMapDomainToResponseWithProject() {
        Project project = project();
        Meal meal = MealMapper.toDomain(mealRequest(null, MealType.LUNCH), restaurant(project));

        MealResponseDTO response = MealMapper.toResponse(meal);

        assertThat(response.project().id()).isEqualTo(project.getId());
        assertThat(response.project().os()).isEqualTo("OS-1234");
        assertThat(response.restaurant().name()).isEqualTo("Restaurante do Ze");
        assertThat(response.mealType()).isEqualTo(MealType.LUNCH);
        assertThat(response.lunchQuantity()).isEqualTo(10);
        assertThat(response.lunchPrice()).isEqualByComparingTo("25.00");
        assertThat(response.dinnerQuantity()).isNull();
        assertThat(response.dinnerPrice()).isNull();
        assertThat(response.dailyPrice()).isEqualByComparingTo("250.00");
        // obra sem data de termino
        assertThat(response.workingDays()).isNull();
        assertThat(response.totalPrice()).isNull();
    }

    @Test
    void shouldMapSummaryWithEstimatedTotal() {
        Project project = new Project(null, "OS-2026", null, costCenter(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 16), client(), false);
        Meal meal = MealMapper.toDomain(mealRequest(null, MealType.LUNCH_AND_DINNER), restaurant(project));

        MealSummaryDTO summary = MealMapper.toSummary(meal);

        assertThat(summary.lunchQuantity()).isEqualTo(10);
        assertThat(summary.dinnerQuantity()).isEqualTo(8);
        assertThat(summary.dailyPrice()).isEqualByComparingTo("426.00");
        assertThat(summary.workingDays()).isEqualTo(10);
        assertThat(summary.totalPrice()).isEqualByComparingTo("4260.00");
        assertThat(MealMapper.toSummary(null)).isNull();
    }

    @Test
    void shouldMapDeliverySodaBreakdownAndProjections() {
        Project project = new Project(null, "OS-2026", null, costCenter(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 16), client(), false);
        Meal meal = MealMapper.toDomain(mealRequest(null, MealType.LUNCH_AND_DINNER),
                restaurant(project, new BigDecimal("8.00"), new BigDecimal("5.00")));

        MealResponseDTO response = MealMapper.toResponse(meal);

        assertThat(response.restaurant().deliveryFee()).isEqualByComparingTo("8.00");
        assertThat(response.restaurant().sodaPrice()).isEqualByComparingTo("5.00");
        assertThat(response.sodaPrice()).isEqualByComparingTo("5.00");
        assertThat(response.deliveriesPerDay()).isEqualTo(2);
        assertThat(response.mealsDailyPrice()).isEqualByComparingTo("426.00");
        assertThat(response.deliveryDailyPrice()).isEqualByComparingTo("16.00");
        assertThat(response.sodaDailyPrice()).isEqualByComparingTo("10.00");
        assertThat(response.dailyPrice()).isEqualByComparingTo("452.00");
        assertThat(response.totalPrice()).isEqualByComparingTo("4520.00");
        assertThat(response.projections()).extracting(MealProjectionDTO::period).containsExactly(
                BillingPeriod.DAY, BillingPeriod.WEEK, BillingPeriod.FORTNIGHT, BillingPeriod.MONTH);
        MealProjectionDTO month = response.projections().get(3);
        assertThat(month.workingDays()).isEqualTo(22);
        assertThat(month.lunchMeals()).isEqualTo(220);
        assertThat(month.dinnerMeals()).isEqualTo(176);
        assertThat(month.deliveries()).isEqualTo(44);
        assertThat(month.sodas()).isEqualTo(44);
        assertThat(month.price()).isEqualByComparingTo("9944.00");
        assertThat(MealMapper.toSummary(meal).projections()).isEqualTo(response.projections());
    }

    @Test
    void shouldKeepIdWhenMappingOverExisting() {
        Meal existing = MealMapper.toDomain(mealRequest(null, MealType.LUNCH), restaurant(project()));

        Meal updated = MealMapper.toDomain(existing, mealRequest(null, MealType.DINNER), existing.getRestaurant());

        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getMealType()).isEqualTo(MealType.DINNER);
        assertThat(updated.getLunchQuantity()).isNull();
        assertThat(updated.getDinnerQuantity()).isEqualTo(8);
    }

    @Test
    void shouldMapAdjustmentsAndTotalsWithAdjustments() {
        Project project = new Project(null, "OS-2026", null, costCenter(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 16), client(), false);
        Meal meal = MealMapper.toDomain(mealRequest(null, MealType.LUNCH_AND_DINNER),
                restaurant(project, new BigDecimal("8.00"), new BigDecimal("5.00")));
        meal.addAdjustment(new MealAdjustment(null, LocalDate.of(2026, 10, 12), MealAdjustment.Type.NO_SERVICE,
                null, null, "Feriado"));
        meal.addAdjustment(new MealAdjustment(null, LocalDate.of(2026, 10, 10), MealAdjustment.Type.EXTRA_DAY,
                6, 0, null));

        MealResponseDTO response = MealMapper.toResponse(meal);
        MealSummaryDTO summary = MealMapper.toSummary(meal);

        assertThat(response.workingDays()).isEqualTo(10);
        assertThat(response.serviceDays()).isEqualTo(9);
        assertThat(response.noServiceDays()).isEqualTo(1);
        assertThat(response.extraDays()).isEqualTo(1);
        assertThat(response.extraDaysPrice()).isEqualByComparingTo("163.00");
        assertThat(response.totalPrice()).isEqualByComparingTo("4231.00");
        assertThat(summary.serviceDays()).isEqualTo(9);
        assertThat(summary.totalPrice()).isEqualByComparingTo("4231.00");
        assertThat(response.adjustments()).extracting(MealAdjustmentResponseDTO::date)
                .containsExactly(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        MealAdjustmentResponseDTO extra = response.adjustments().get(0);
        assertThat(extra.weekday()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(extra.type()).isEqualTo(MealAdjustment.Type.EXTRA_DAY);
        assertThat(extra.deliveries()).isEqualTo(1);
        assertThat(extra.sodas()).isEqualTo(1);
        assertThat(extra.price()).isEqualByComparingTo("163.00");
        assertThat(extra.inPeriod()).isTrue();
        MealAdjustmentResponseDTO holiday = response.adjustments().get(1);
        assertThat(holiday.note()).isEqualTo("Feriado");
        assertThat(holiday.price()).isEqualByComparingTo("0");
    }

    @Test
    void shouldKeepAdjustmentsWhenMappingOverExisting() {
        Project project = new Project(null, "OS-2026", null, costCenter(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 16), client(), false);
        Meal existing = MealMapper.toDomain(mealRequest(null, MealType.LUNCH), restaurant(project));
        MealAdjustment holiday = existing.addAdjustment(new MealAdjustment(null, LocalDate.of(2026, 10, 12),
                MealAdjustment.Type.NO_SERVICE, null, null, null));

        Meal updated = MealMapper.toDomain(existing, mealRequest(null, MealType.DINNER), existing.getRestaurant());

        assertThat(updated.getAdjustments()).containsExactly(holiday);
        assertThat(updated.getServiceDays()).isEqualTo(9);
    }
}
