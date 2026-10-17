package com.longobuccodev.app_adm_obras.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fechamento de um dia de uma refeicao ou, somado, de um restaurante.
 */
public final class DayClosing {

    public enum Status {
        /** Dia util com o fornecimento contratado. */
        SERVICE,
        /** Dia util sem fornecimento (ajuste). */
        NO_SERVICE,
        /** Sabado ou domingo com fornecimento (ajuste). */
        EXTRA_DAY,
        /** Sabado ou domingo sem ajuste. */
        OFF,
        /** Antes do inicio ou depois do termino da obra. */
        OUT_OF_PERIOD
    }

    private final LocalDate date;
    private final Status status;
    private final boolean partialNoService;
    private final int lunchMeals;
    private final int dinnerMeals;
    private final int deliveries;
    private final int sodas;
    private final BigDecimal price;
    private final List<UUID> adjustmentIds;

    /**
     * @param partialNoService no restaurante: parte das refeicoes sem fornecimento num dia em que outra fornece
     * @param adjustmentIds    ajustes por data que definem o dia (dia extra ou sem fornecimento)
     */
    public DayClosing(LocalDate date, Status status, boolean partialNoService, int lunchMeals, int dinnerMeals,
                      int deliveries, int sodas, BigDecimal price, List<UUID> adjustmentIds) {
        this.date = date;
        this.status = status;
        this.partialNoService = partialNoService;
        this.lunchMeals = lunchMeals;
        this.dinnerMeals = dinnerMeals;
        this.deliveries = deliveries;
        this.sodas = sodas;
        this.price = price;
        this.adjustmentIds = List.copyOf(adjustmentIds);
    }

    public static DayClosing empty(LocalDate date, Status status) {
        return new DayClosing(date, status, false, 0, 0, 0, 0, BigDecimal.ZERO, List.of());
    }

    /** Dia que fornece algo: fornecimento normal ou dia extra. */
    public boolean isServiceDay() {
        return status == Status.SERVICE || status == Status.EXTRA_DAY;
    }

    public LocalDate date() {
        return date;
    }

    public Status status() {
        return status;
    }

    public boolean partialNoService() {
        return partialNoService;
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

    public List<UUID> adjustmentIds() {
        return adjustmentIds;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DayClosing other
                && Objects.equals(date, other.date)
                && status == other.status
                && partialNoService == other.partialNoService
                && lunchMeals == other.lunchMeals
                && dinnerMeals == other.dinnerMeals
                && deliveries == other.deliveries
                && sodas == other.sodas
                && Objects.equals(price, other.price)
                && adjustmentIds.equals(other.adjustmentIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, status, partialNoService, lunchMeals, dinnerMeals, deliveries, sodas, price,
                adjustmentIds);
    }

    @Override
    public String toString() {
        return "DayClosing[date=" + date + ", status=" + status + ", partialNoService=" + partialNoService
                + ", lunchMeals=" + lunchMeals + ", dinnerMeals=" + dinnerMeals + ", deliveries=" + deliveries
                + ", sodas=" + sodas + ", price=" + price + ", adjustmentIds=" + adjustmentIds + "]";
    }
}
