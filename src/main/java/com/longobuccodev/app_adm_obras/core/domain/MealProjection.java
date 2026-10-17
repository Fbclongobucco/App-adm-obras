package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Meal.BillingPeriod;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Quantidades e valor de uma refeicao num periodo de fechamento: valores diarios x dias uteis do periodo.
 */
public final class MealProjection {

    private final BillingPeriod period;
    private final int workingDays;
    private final int lunchMeals;
    private final int dinnerMeals;
    private final int deliveries;
    private final int sodas;
    private final BigDecimal price;

    /**
     * @param lunchMeals  marmitas de almoco (0 se a refeicao nao serve almoco)
     * @param dinnerMeals marmitas de jantar (0 se a refeicao nao serve jantar)
     * @param deliveries  entregas (uma por refeicao servida por dia)
     * @param sodas       refrigerantes (um por entrega; 0 se a refeicao nao tem refrigerante)
     * @param price       custo por dia (marmitas, entrega e refrigerante) x dias uteis
     */
    public MealProjection(BillingPeriod period, int workingDays, int lunchMeals, int dinnerMeals, int deliveries,
                          int sodas, BigDecimal price) {
        this.period = period;
        this.workingDays = workingDays;
        this.lunchMeals = lunchMeals;
        this.dinnerMeals = dinnerMeals;
        this.deliveries = deliveries;
        this.sodas = sodas;
        this.price = price;
    }

    public BillingPeriod period() {
        return period;
    }

    public int workingDays() {
        return workingDays;
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
        return o instanceof MealProjection other
                && period == other.period
                && workingDays == other.workingDays
                && lunchMeals == other.lunchMeals
                && dinnerMeals == other.dinnerMeals
                && deliveries == other.deliveries
                && sodas == other.sodas
                && Objects.equals(price, other.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, workingDays, lunchMeals, dinnerMeals, deliveries, sodas, price);
    }

    @Override
    public String toString() {
        return "MealProjection[period=" + period + ", workingDays=" + workingDays + ", lunchMeals=" + lunchMeals
                + ", dinnerMeals=" + dinnerMeals + ", deliveries=" + deliveries + ", sodas=" + sodas
                + ", price=" + price + "]";
    }
}
