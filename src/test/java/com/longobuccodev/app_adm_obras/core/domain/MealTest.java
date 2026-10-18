package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Meal.BillingPeriod;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.exception.InvalidMealException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MealTest {

    private static final BigDecimal LUNCH_PRICE = new BigDecimal("25.00");
    private static final BigDecimal DINNER_PRICE = new BigDecimal("22.00");
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("8.00");
    private static final BigDecimal SODA_PRICE = new BigDecimal("5.00");

    /** Almoco 10 x 25.00 e jantar 8 x 22.00 numa obra de 2026-10-05 (segunda) a 2026-10-16 (sexta). */
    private static Meal lunchAndDinner() {
        return new Meal(null, restaurant(LocalDate.of(2026, 10, 16)), MealType.LUNCH_AND_DINNER, null,
                10, LUNCH_PRICE, 8, DINNER_PRICE);
    }

    private static Meal meal(MealType type, Integer lunchQuantity, BigDecimal lunchPrice, Integer dinnerQuantity,
                             BigDecimal dinnerPrice) {
        return new Meal(null, restaurant(null), type, false, lunchQuantity, lunchPrice, dinnerQuantity, dinnerPrice);
    }

    /**
     * Almoco 10 x 25.00 e jantar 8 x 22.00 num restaurante com taxa de entrega 8.00 e refrigerante 5.00,
     * obra de 10 dias uteis.
     */
    private static Meal lunchAndDinnerWithDeliveryAndSoda() {
        return new Meal(null, restaurant(LocalDate.of(2026, 10, 16), DELIVERY_FEE, SODA_PRICE),
                MealType.LUNCH_AND_DINNER, null, 10, LUNCH_PRICE, 8, DINNER_PRICE);
    }

    @Test
    void shouldCountOneDeliveryPerServedMeal() {
        assertThat(MealType.LUNCH.deliveriesPerDay()).isEqualTo(1);
        assertThat(MealType.DINNER.deliveriesPerDay()).isEqualTo(1);
        assertThat(MealType.LUNCH_AND_DINNER.deliveriesPerDay()).isEqualTo(2);
        assertThat(lunchAndDinner().getDeliveriesPerDay()).isEqualTo(2);
    }

    @Test
    void shouldBreakDailyPriceIntoMealsDeliveryAndSoda() {
        Meal meal = lunchAndDinnerWithDeliveryAndSoda();

        assertThat(meal.getSodaPrice()).isEqualByComparingTo("5.00");
        assertThat(meal.getMealsDailyPrice()).isEqualByComparingTo("426.00");
        assertThat(meal.getDeliveryDailyPrice()).isEqualByComparingTo("16.00");
        assertThat(meal.getSodaDailyPrice()).isEqualByComparingTo("10.00");
        assertThat(meal.getDailyPrice()).isEqualByComparingTo("452.00");
        assertThat(meal.getWorkingDays()).isEqualTo(10);
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("4520.00");
    }

    @Test
    void shouldChargeOneDeliveryForLunchOnlyWithoutSoda() {
        Meal meal = new Meal(null, restaurant(null, DELIVERY_FEE), MealType.LUNCH, false,
                10, LUNCH_PRICE, null, null);

        assertThat(meal.getDeliveriesPerDay()).isEqualTo(1);
        assertThat(meal.getMealsDailyPrice()).isEqualByComparingTo("250.00");
        assertThat(meal.getDeliveryDailyPrice()).isEqualByComparingTo("8.00");
        assertThat(meal.getSodaDailyPrice()).isEqualByComparingTo("0");
        assertThat(meal.getDailyPrice()).isEqualByComparingTo("258.00");
        assertThat(meal.projection(BillingPeriod.DAY).deliveries()).isEqualTo(1);
        assertThat(meal.projection(BillingPeriod.DAY).sodas()).isZero();
    }

    @Test
    void shouldKeepCurrentValuesWithoutFeeAndSoda() {
        Meal meal = lunchAndDinner();

        assertThat(meal.getSodaPrice()).isNull();
        assertThat(meal.getDeliveryDailyPrice()).isEqualByComparingTo("0");
        assertThat(meal.getSodaDailyPrice()).isEqualByComparingTo("0");
        assertThat(meal.getMealsDailyPrice()).isEqualByComparingTo("426.00");
        assertThat(meal.getDailyPrice()).isEqualByComparingTo("426.00");
        assertThat(meal.projection(BillingPeriod.WEEK).sodas()).isZero();
        assertThat(meal.projection(BillingPeriod.WEEK).price()).isEqualByComparingTo("2130.00");
    }

    @Test
    void shouldFollowRestaurantDeliveryFeeChanges() {
        Meal meal = lunchAndDinner();

        meal.getRestaurant().setLunchDeliveryFee(DELIVERY_FEE);
        meal.getRestaurant().setDinnerDeliveryFee(DELIVERY_FEE);

        assertThat(meal.getDeliveryDailyPrice()).isEqualByComparingTo("16.00");
        assertThat(meal.getDailyPrice()).isEqualByComparingTo("442.00");
    }

    @Test
    void shouldProjectEachBillingPeriod() {
        Meal meal = lunchAndDinnerWithDeliveryAndSoda();

        assertThat(meal.projection(BillingPeriod.DAY)).isEqualTo(
                new MealProjection(BillingPeriod.DAY, 1, 10, 8, 2, 2, meal.getDailyPrice()));
        MealProjection week = meal.projection(BillingPeriod.WEEK);
        assertThat(week.workingDays()).isEqualTo(5);
        assertThat(week.lunchMeals()).isEqualTo(50);
        assertThat(week.dinnerMeals()).isEqualTo(40);
        assertThat(week.deliveries()).isEqualTo(10);
        assertThat(week.sodas()).isEqualTo(10);
        assertThat(week.price()).isEqualByComparingTo("2260.00");
        assertThat(meal.projection(BillingPeriod.FORTNIGHT).price()).isEqualByComparingTo("4520.00");
        MealProjection month = meal.projection(BillingPeriod.MONTH);
        assertThat(month.workingDays()).isEqualTo(22);
        assertThat(month.lunchMeals()).isEqualTo(220);
        assertThat(month.dinnerMeals()).isEqualTo(176);
        assertThat(month.deliveries()).isEqualTo(44);
        assertThat(month.sodas()).isEqualTo(44);
        assertThat(month.price()).isEqualByComparingTo("9944.00");
    }

    @Test
    void shouldListProjectionsInPeriodOrder() {
        List<MealProjection> projections = lunchAndDinnerWithDeliveryAndSoda().getProjections();

        assertThat(projections).extracting(MealProjection::period).containsExactly(
                BillingPeriod.DAY, BillingPeriod.WEEK, BillingPeriod.FORTNIGHT, BillingPeriod.MONTH);
        assertThat(projections).extracting(MealProjection::workingDays).containsExactly(1, 5, 10, 22);
    }

    @Test
    void shouldProjectZeroForMealNotServed() {
        Meal lunch = meal(MealType.LUNCH, 10, LUNCH_PRICE, 8, DINNER_PRICE);

        MealProjection month = lunch.projection(BillingPeriod.MONTH);

        assertThat(month.lunchMeals()).isEqualTo(220);
        assertThat(month.dinnerMeals()).isZero();
        assertThat(month.deliveries()).isEqualTo(22);
        assertThat(month.sodas()).isZero();
    }

    @Test
    void shouldReadSodaPriceFromRestaurant() {
        Meal meal = lunchAndDinnerWithDeliveryAndSoda();

        assertThat(meal.getSodaPrice()).isSameAs(meal.getRestaurant().getSodaPrice());
        assertThat(meal.getDailyPrice()).isEqualByComparingTo("452.00");
    }

    @Test
    void shouldFollowRestaurantSodaPriceChangesOnEveryMeal() {
        Restaurant restaurant = restaurant(LocalDate.of(2026, 10, 16), DELIVERY_FEE, SODA_PRICE);
        Meal lunch = new Meal(null, restaurant, MealType.LUNCH, false, 10, LUNCH_PRICE, null, null);
        Meal dinner = new Meal(null, restaurant, MealType.DINNER, false, null, null, 8, DINNER_PRICE);

        restaurant.setSodaPrice(new BigDecimal("6.00"));

        assertThat(lunch.getSodaPrice()).isEqualByComparingTo("6.00");
        assertThat(lunch.getSodaDailyPrice()).isEqualByComparingTo("6.00");
        assertThat(lunch.getDailyPrice()).isEqualByComparingTo("264.00");
        assertThat(dinner.getSodaDailyPrice()).isEqualByComparingTo("6.00");
        assertThat(dinner.projection(BillingPeriod.WEEK).price()).isEqualByComparingTo("950.00");

        restaurant.setSodaPrice(null);

        assertThat(lunch.getSodaPrice()).isNull();
        assertThat(lunch.getSodaDailyPrice()).isEqualByComparingTo("0");
        assertThat(lunch.projection(BillingPeriod.MONTH).sodas()).isZero();
        assertThat(dinner.getDailyPrice()).isEqualByComparingTo("184.00");
    }

    @Test
    void shouldCalculateDailyPriceWorkingDaysAndTotalFromProject() {
        Meal meal = lunchAndDinner();

        assertThat(meal.getRestaurant().getName()).isEqualTo("Restaurante do Ze");
        assertThat(meal.getLunchQuantity()).isEqualTo(10);
        assertThat(meal.getLunchPrice()).isEqualByComparingTo("25.00");
        assertThat(meal.getDinnerQuantity()).isEqualTo(8);
        assertThat(meal.getDinnerPrice()).isEqualByComparingTo("22.00");
        assertThat(meal.getDailyPrice()).isEqualByComparingTo("426.00");
        assertThat(meal.getWorkingDays()).isEqualTo(10);
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("4260.00");
    }

    @Test
    void shouldHaveNoWorkingDaysNorTotalWithoutProjectEndDate() {
        Meal meal = meal(MealType.LUNCH, 10, LUNCH_PRICE, null, null);

        assertThat(meal.getDailyPrice()).isEqualByComparingTo("250.00");
        assertThat(meal.getWorkingDays()).isNull();
        assertThat(meal.getTotalPrice()).isNull();
    }

    @Test
    void shouldFollowProjectEndDateChanges() {
        Meal meal = lunchAndDinner();

        meal.getProject().setEndDate(LocalDate.of(2026, 10, 9));

        assertThat(meal.getWorkingDays()).isEqualTo(5);
        assertThat(meal.getTotalPrice()).isEqualByComparingTo("2130.00");
    }

    @Test
    void shouldIgnoreFieldsOfMealNotServed() {
        Meal lunch = meal(MealType.LUNCH, 10, LUNCH_PRICE, 8, DINNER_PRICE);
        Meal dinner = meal(MealType.DINNER, 0, null, 8, DINNER_PRICE);

        assertThat(lunch.getDinnerQuantity()).isNull();
        assertThat(lunch.getDinnerPrice()).isNull();
        assertThat(lunch.getDailyPrice()).isEqualByComparingTo("250.00");
        assertThat(dinner.getLunchQuantity()).isNull();
        assertThat(dinner.getLunchPrice()).isNull();
        assertThat(dinner.getDailyPrice()).isEqualByComparingTo("176.00");
    }

    @Test
    void shouldClearFieldsWhenTypeChanges() {
        Meal meal = lunchAndDinner();

        meal.configure(MealType.DINNER, 10, LUNCH_PRICE, 5, DINNER_PRICE);

        assertThat(meal.getMealType()).isEqualTo(MealType.DINNER);
        assertThat(meal.getLunchQuantity()).isNull();
        assertThat(meal.getLunchPrice()).isNull();
        assertThat(meal.getDailyPrice()).isEqualByComparingTo("110.00");
    }

    @Test
    void shouldKeepPreviousStateWhenConfigurationIsInvalid() {
        Meal meal = lunchAndDinner();

        assertThatThrownBy(() -> meal.configure(MealType.LUNCH, 0, LUNCH_PRICE, null, null))
                .isInstanceOf(InvalidMealException.class);

        assertThat(meal.getMealType()).isEqualTo(MealType.LUNCH_AND_DINNER);
        assertThat(meal.getDailyPrice()).isEqualByComparingTo("426.00");
    }

    @Test
    void shouldRejectInvalidLunchQuantity() {
        for (Integer quantity : new Integer[]{null, 0, 10001}) {
            assertThatThrownBy(() -> meal(MealType.LUNCH, quantity, LUNCH_PRICE, null, null))
                    .isInstanceOf(InvalidMealException.class)
                    .extracting("errorCode").isEqualTo("meal.invalid.lunch_quantity");
        }
        assertThat(meal(MealType.LUNCH, 1, LUNCH_PRICE, null, null).getLunchQuantity()).isEqualTo(1);
        assertThat(meal(MealType.LUNCH, 10000, LUNCH_PRICE, null, null).getLunchQuantity()).isEqualTo(10000);
    }

    @Test
    void shouldRejectInvalidLunchPrice() {
        for (BigDecimal price : new BigDecimal[]{null, BigDecimal.ZERO, new BigDecimal("-1.00")}) {
            assertThatThrownBy(() -> meal(MealType.LUNCH_AND_DINNER, 10, price, 8, DINNER_PRICE))
                    .isInstanceOf(InvalidMealException.class)
                    .extracting("errorCode").isEqualTo("meal.invalid.lunch_price");
        }
    }

    @Test
    void shouldRejectInvalidDinnerQuantity() {
        for (Integer quantity : new Integer[]{null, 0, 10001}) {
            assertThatThrownBy(() -> meal(MealType.DINNER, null, null, quantity, DINNER_PRICE))
                    .isInstanceOf(InvalidMealException.class)
                    .extracting("errorCode").isEqualTo("meal.invalid.dinner_quantity");
        }
    }

    @Test
    void shouldRejectInvalidDinnerPrice() {
        for (BigDecimal price : new BigDecimal[]{null, BigDecimal.ZERO, new BigDecimal("-1.00")}) {
            assertThatThrownBy(() -> meal(MealType.LUNCH_AND_DINNER, 10, LUNCH_PRICE, 8, price))
                    .isInstanceOf(InvalidMealException.class)
                    .extracting("errorCode").isEqualTo("meal.invalid.dinner_price");
        }
    }

    @Test
    void shouldRejectMissingMealType() {
        assertThatThrownBy(() -> meal(null, 10, LUNCH_PRICE, 8, DINNER_PRICE))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.type");
    }

    @Test
    void shouldDetectOverlappingCoverage() {
        Meal lunch = meal(MealType.LUNCH, 10, LUNCH_PRICE, null, null);
        Meal dinner = meal(MealType.DINNER, null, null, 8, DINNER_PRICE);
        Meal both = meal(MealType.LUNCH_AND_DINNER, 10, LUNCH_PRICE, 8, DINNER_PRICE);

        assertThat(lunch.overlaps(dinner)).isFalse();
        assertThat(lunch.overlaps(meal(MealType.LUNCH, 1, LUNCH_PRICE, null, null))).isTrue();
        assertThat(both.overlaps(lunch)).isTrue();
        assertThat(both.overlaps(dinner)).isTrue();
        assertThat(dinner.overlaps(both)).isTrue();
        assertThat(both.overlaps(both)).isFalse();
        assertThat(lunch.overlaps(null)).isFalse();
    }

    @Test
    void shouldNotOverlapItselfWithSameId() {
        Meal meal = meal(MealType.LUNCH, 10, LUNCH_PRICE, null, null);
        Meal edited = new Meal(meal.getId(), meal.getRestaurant(), MealType.LUNCH_AND_DINNER, false,
                10, LUNCH_PRICE, 8, DINNER_PRICE);

        assertThat(edited.overlaps(meal)).isFalse();
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();

        assertThat(new Meal(id, restaurant(null), MealType.LUNCH, false, 1, BigDecimal.ONE, null, null).getId())
                .isEqualTo(id);
        assertThat(lunchAndDinner().getId()).isNotNull();
    }

    @Test
    void shouldCompareById() {
        UUID id = UUID.randomUUID();
        Meal first = lunchAndDinner();
        Meal second = lunchAndDinner();

        assertThat(first).isNotEqualTo(second);

        first.setId(id);
        second.setId(id);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldRequireRestaurant() {
        Meal meal = lunchAndDinner();

        assertThatThrownBy(() -> meal.setRestaurant(null))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.restaurant");
        assertThatThrownBy(() -> new Meal(null, null, MealType.LUNCH, false, 1, BigDecimal.ONE, null, null))
                .isInstanceOf(InvalidMealException.class)
                .extracting("errorCode").isEqualTo("meal.invalid.restaurant");
    }

    @Test
    void shouldTakeProjectFromRestaurant() {
        Restaurant restaurant = restaurant(null);

        Meal meal = new Meal(null, restaurant, MealType.DINNER, false, null, null, 1, BigDecimal.ONE);

        assertThat(meal.getProject()).isSameAs(restaurant.getProject());
    }

    @Test
    void shouldDefaultBilledToFalse() {
        Meal meal = lunchAndDinner();

        assertThat(meal.getBilled()).isFalse();

        meal.setBilled(true);
        assertThat(meal.isBilled()).isTrue();

        meal.setBilled(null);
        assertThat(meal.isBilled()).isFalse();
        assertThat(meal.getBilled()).isFalse();
    }

    private static Restaurant restaurant(LocalDate endDate) {
        return restaurant(endDate, null);
    }

    private static Restaurant restaurant(LocalDate endDate, BigDecimal deliveryFee) {
        return restaurant(endDate, deliveryFee, null);
    }

    private static Restaurant restaurant(LocalDate endDate, BigDecimal deliveryFee, BigDecimal sodaPrice) {
        Project project = new Project(null, "OS-100", null, new CostCenter(null, "Centro Norte", "11222333000181"),
                LocalDate.of(2026, 10, 5), endDate,
                new Client(null, "Cliente Alfa", "alfa@exemplo.test", "1133334444",
                        new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000")),
                false);
        return new Restaurant(null, " Restaurante  do  Ze ", null, null, project, deliveryFee, sodaPrice);
    }
}
