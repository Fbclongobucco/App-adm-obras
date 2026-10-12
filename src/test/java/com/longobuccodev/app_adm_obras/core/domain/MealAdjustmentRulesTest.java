package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment.Type;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.exception.InvalidMealAdjustmentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras de inclusao de ajustes na refeicao e total estimado com ajustes (change refeicao-ajustes-por-data). */
class MealAdjustmentRulesTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = LocalDate.of(2026, 10, 16);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 10);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 11);
    private static final LocalDate MONDAY_HOLIDAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);

    /**
     * Obra de 2026-10-05 a 2026-10-16; LUNCH_AND_DINNER almoco 10 x 25.00 e jantar 8 x 22.00; restaurante com
     * taxa 8.00 e refrigerante 5.00 (452.00 por dia).
     */
    static Meal lunchAndDinner452() {
        return meal(MealType.LUNCH_AND_DINNER, END);
    }

    private static Meal meal(MealType type, LocalDate endDate) {
        Project project = new Project(null, "OS-2026", null, new CostCenter(null, "Centro Norte", "11222333000181"),
                START, endDate,
                new Client(null, "Cliente Alfa", null, "1133334444",
                        new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000")),
                false);
        Restaurant restaurant = new Restaurant(null, "Cantina da Obra", null, null, project,
                new BigDecimal("8.00"), new BigDecimal("5.00"));
        return new Meal(null, restaurant, type, false, 10, new BigDecimal("25.00"), 8, new BigDecimal("22.00"));
    }

    private static MealAdjustment extraDay(LocalDate date, Integer lunch, Integer dinner) {
        return new MealAdjustment(null, date, Type.EXTRA_DAY, lunch, dinner, null);
    }

    private static MealAdjustment noService(LocalDate date) {
        return new MealAdjustment(null, date, Type.NO_SERVICE, null, null, "Feriado de N. Sra. Aparecida");
    }

    @Test
    @DisplayName("cenario da spec: 452,00/dia, 1 sem fornecimento, extra de 163,00 -> total 4231,00")
    void specScenario() {
        Meal meal = lunchAndDinner452();

        meal.addAdjustment(noService(MONDAY_HOLIDAY));
        MealAdjustment extra = meal.addAdjustment(extraDay(SATURDAY, 6, 0));

        assertThat(meal.getDailyPrice()).isEqualByComparingTo("452.00");
        assertThat(extra.deliveries(meal)).isEqualTo(1);
        assertThat(extra.sodas(meal)).isEqualTo(1);
        assertThat(extra.price(meal)).isEqualByComparingTo("163.00");
        assertThat(meal.getWorkingDays()).isEqualTo(10);
        assertThat(meal.getNoServiceDays()).isEqualTo(1);
        assertThat(meal.getServiceDays()).isEqualTo(9);
        assertThat(meal.getExtraDays()).isEqualTo(1);
        assertThat(meal.getExtraDaysPrice()).isEqualByComparingTo("163.00");
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("4231.00");
        assertThat(meal.getAdjustments()).extracting(MealAdjustment::getDate)
                .containsExactly(SATURDAY, MONDAY_HOLIDAY);
        assertThat(meal.getAdjustments()).allMatch(meal::isInPeriod);
    }

    @Test
    @DisplayName("sem ajustes: dias de fornecimento = dias uteis e total igual ao anterior")
    void withoutAdjustments() {
        Meal meal = lunchAndDinner452();

        assertThat(meal.getServiceDays()).isEqualTo(10);
        assertThat(meal.getNoServiceDays()).isZero();
        assertThat(meal.getExtraDays()).isZero();
        assertThat(meal.getExtraDaysPrice()).isEqualByComparingTo("0");
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("4520.00");
    }

    @Test
    @DisplayName("data fora do periodo da obra (antes do inicio, depois do termino ou ausente)")
    void dateOutsideProject() {
        Meal meal = lunchAndDinner452();

        for (LocalDate date : new LocalDate[]{LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 20), null}) {
            assertThatThrownBy(() -> meal.addAdjustment(noService(date)))
                    .as("date=" + date)
                    .isInstanceOf(InvalidMealAdjustmentException.class)
                    .extracting("errorCode").isEqualTo("meal.adjustment.invalid.date");
        }
        // limites inclusivos
        meal.addAdjustment(noService(START));
        meal.addAdjustment(noService(END));
        assertThat(meal.getNoServiceDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("obra sem termino aceita datas a partir do inicio, sem dias de fornecimento nem total")
    void projectWithoutEndDate() {
        Meal meal = meal(MealType.LUNCH_AND_DINNER, null);

        meal.addAdjustment(noService(LocalDate.of(2026, 12, 7)));
        meal.addAdjustment(extraDay(LocalDate.of(2026, 12, 5), 6, 0));

        assertThat(meal.getNoServiceDays()).isEqualTo(1);
        assertThat(meal.getExtraDaysPrice()).isEqualByComparingTo("163.00");
        assertThat(meal.getServiceDays()).isNull();
        assertThat(meal.getTotalPrice()).isNull();
        assertThatThrownBy(() -> meal.addAdjustment(noService(LocalDate.of(2026, 10, 2))))
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.date");
    }

    @Test
    @DisplayName("data repetida: 409 meal.adjustment.duplicate.date, inclusive com outro tipo")
    void duplicateDate() {
        Meal meal = lunchAndDinner452();
        meal.addAdjustment(noService(MONDAY_HOLIDAY));

        assertThatThrownBy(() -> meal.addAdjustment(noService(MONDAY_HOLIDAY)))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("meal.adjustment.duplicate.date");
        // repetida e verificada antes do dia da semana
        assertThatThrownBy(() -> meal.addAdjustment(extraDay(MONDAY_HOLIDAY, 6, 0)))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("meal.adjustment.duplicate.date");
        assertThat(meal.getAdjustments()).hasSize(1);
    }

    @Test
    @DisplayName("ordem: data fora do periodo antes de repetida; repetida antes de dia da semana e quantidades")
    void validationOrder() {
        Meal meal = meal(MealType.LUNCH_AND_DINNER, END);
        meal.addAdjustment(extraDay(SATURDAY, 6, 0));
        meal.getProject().setEndDate(LocalDate.of(2026, 10, 9));

        // 10/10 agora esta fora do periodo e ja existe: vale a regra de periodo
        assertThatThrownBy(() -> meal.addAdjustment(extraDay(SATURDAY, 1, 1)))
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.date");
        meal.getProject().setEndDate(END);
        // repetida, em sabado, com NO_SERVICE e quantidades invalidas: vale a repetida
        assertThatThrownBy(() -> meal.addAdjustment(new MealAdjustment(null, SATURDAY, Type.NO_SERVICE, 3, 3, null)))
                .extracting("errorCode").isEqualTo("meal.adjustment.duplicate.date");
        // dia extra em dia util com quantidades zeradas: vale o dia da semana
        assertThatThrownBy(() -> meal.addAdjustment(extraDay(WEDNESDAY, 0, 0)))
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.extra_day");
        // sem fornecimento no domingo com quantidades: vale o dia da semana
        assertThatThrownBy(() -> meal.addAdjustment(new MealAdjustment(null, SUNDAY, Type.NO_SERVICE, 3, null, null)))
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.no_service");
    }

    @Test
    @DisplayName("dia extra so em sabado ou domingo")
    void extraDayOnWeekdays() {
        Meal meal = lunchAndDinner452();

        assertThatThrownBy(() -> meal.addAdjustment(extraDay(WEDNESDAY, 6, 0)))
                .isInstanceOf(InvalidMealAdjustmentException.class)
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.extra_day");
        meal.addAdjustment(extraDay(SUNDAY, 0, 5));
        assertThat(meal.getExtraDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("sem fornecimento so de segunda a sexta")
    void noServiceOnWeekend() {
        Meal meal = lunchAndDinner452();

        for (LocalDate date : new LocalDate[]{SATURDAY, SUNDAY}) {
            assertThatThrownBy(() -> meal.addAdjustment(noService(date)))
                    .isInstanceOf(InvalidMealAdjustmentException.class)
                    .extracting("errorCode").isEqualTo("meal.adjustment.invalid.no_service");
        }
        assertThat(meal.getAdjustments()).isEmpty();
    }

    @Test
    @DisplayName("quantidades do dia extra: obrigatorias das refeicoes servidas, 0 a 10000, ao menos uma > 0")
    void extraDayQuantities() {
        Meal meal = lunchAndDinner452();
        Integer[][] invalid = {{0, 0}, {6, null}, {null, 4}, {null, null}, {-1, 4}, {10001, 0}, {6, 10001}};

        for (Integer[] quantities : invalid) {
            assertThatThrownBy(() -> meal.addAdjustment(extraDay(SATURDAY, quantities[0], quantities[1])))
                    .as("almoco=%s jantar=%s", quantities[0], quantities[1])
                    .isInstanceOf(InvalidMealAdjustmentException.class)
                    .extracting("errorCode").isEqualTo("meal.adjustment.invalid.quantity");
        }
        assertThat(meal.getAdjustments()).isEmpty();
        assertThat(meal.addAdjustment(extraDay(SATURDAY, 10000, 0)).getLunchQuantity()).isEqualTo(10000);
    }

    @Test
    @DisplayName("quantidade de refeicao nao servida pelo tipo e descartada")
    void quantityOfMealNotServedIsDiscarded() {
        Meal lunch = meal(MealType.LUNCH, END);

        MealAdjustment added = lunch.addAdjustment(extraDay(SATURDAY, 6, 99));

        assertThat(added.getLunchQuantity()).isEqualTo(6);
        assertThat(added.getDinnerQuantity()).isNull();
        assertThat(lunch.getAdjustments()).containsExactly(added);
        // LUNCH: 6 x 25 + 8 + 5
        assertThat(lunch.getExtraDaysPrice()).isEqualByComparingTo("163.00");
        assertThatThrownBy(() -> lunch.addAdjustment(extraDay(SUNDAY, 0, 99)))
                .extracting("errorCode").isEqualTo("meal.adjustment.invalid.quantity");
    }

    @Test
    @DisplayName("sem fornecimento nao aceita quantidades")
    void noServiceWithoutQuantities() {
        Meal meal = lunchAndDinner452();

        for (Integer[] quantities : new Integer[][]{{5, null}, {null, 0}, {0, 0}}) {
            assertThatThrownBy(() -> meal.addAdjustment(
                    new MealAdjustment(null, MONDAY_HOLIDAY, Type.NO_SERVICE, quantities[0], quantities[1], null)))
                    .isInstanceOf(InvalidMealAdjustmentException.class)
                    .extracting("errorCode").isEqualTo("meal.adjustment.invalid.quantity");
        }
    }

    @Test
    @DisplayName("ajuste fora do periodo apos mudar o termino fica guardado, inPeriod falso e fora do total")
    void adjustmentsOutsideNewPeriod() {
        Meal meal = lunchAndDinner452();
        MealAdjustment holiday = meal.addAdjustment(noService(MONDAY_HOLIDAY));
        MealAdjustment extra = meal.addAdjustment(extraDay(SATURDAY, 6, 0));

        meal.getProject().setEndDate(LocalDate.of(2026, 10, 9));

        assertThat(meal.getAdjustments()).containsExactly(extra, holiday);
        assertThat(meal.isInPeriod(extra)).isFalse();
        assertThat(meal.isInPeriod(holiday)).isFalse();
        assertThat(meal.getNoServiceDays()).isZero();
        assertThat(meal.getExtraDays()).isZero();
        assertThat(meal.getServiceDays()).isEqualTo(5);
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("2260.00");

        meal.getProject().setEndDate(END);

        assertThat(meal.getTotalPrice()).isEqualByComparingTo("4231.00");
    }

    @Test
    @DisplayName("remover devolve se existia e o total volta a contar o dia util")
    void removeAdjustment() {
        Meal meal = lunchAndDinner452();
        MealAdjustment holiday = meal.addAdjustment(noService(MONDAY_HOLIDAY));

        assertThat(meal.removeAdjustment(holiday.getId())).isTrue();
        assertThat(meal.removeAdjustment(holiday.getId())).isFalse();
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("4520.00");
        assertThat(meal.findAdjustment(holiday.getId())).isEmpty();
    }

    @Test
    @DisplayName("mudar o refrigerante do restaurante muda o valor dos dias extras")
    void extraDayFollowsRestaurantPrices() {
        Meal meal = lunchAndDinner452();
        meal.addAdjustment(extraDay(SATURDAY, 6, 0));

        meal.getRestaurant().setSodaPrice(new BigDecimal("6.00"));

        assertThat(meal.getExtraDaysPrice()).isEqualByComparingTo("164.00");
    }

    @Test
    @DisplayName("total da obra soma o total da refeicao com ajustes")
    void projectTotalFollows() {
        Meal meal = lunchAndDinner452();
        meal.addAdjustment(noService(MONDAY_HOLIDAY));
        meal.addAdjustment(extraDay(SATURDAY, 6, 0));
        Project project = meal.getProject();

        project.addMeal(meal);

        assertThat(project.getTotalPrice()).isEqualByComparingTo("4231.00");
    }

    @Test
    @DisplayName("reidratar nao aplica regras e substitui pelo id")
    void restoreAdjustment() {
        Meal meal = lunchAndDinner452();
        MealAdjustment outside = new MealAdjustment(null, LocalDate.of(2026, 11, 2), Type.NO_SERVICE, null, null, null);

        meal.restoreAdjustment(outside);
        meal.restoreAdjustment(outside);

        assertThat(meal.getAdjustments()).containsExactly(outside);
        assertThat(meal.isInPeriod(outside)).isFalse();
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("4520.00");
    }
}
