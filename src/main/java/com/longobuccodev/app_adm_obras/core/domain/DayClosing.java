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
        /** Dia util com outros numeros (ajuste). */
        CHANGED,
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
    private final UUID mealId;
    private final Status status;
    private final boolean partialNoService;
    private final int lunchMeals;
    private final int dinnerMeals;
    private final int lunchDeliveries;
    private final int dinnerDeliveries;
    private final int sodas;
    private final BigDecimal price;
    private final List<UUID> adjustmentIds;
    private final String note;
    private final List<DayClosing> meals;

    /**
     * @param mealId           refeicao do dia; {@code null} no dia somado do restaurante
     * @param partialNoService no restaurante: parte das refeicoes sem fornecimento num dia em que outra fornece
     * @param adjustmentIds    ajustes por data que definem o dia (dia extra, dia alterado ou sem fornecimento)
     * @param note             observacao do ajuste (no restaurante, a primeira das refeicoes)
     * @param meals            no restaurante, o dia de cada refeicao; vazio no dia de uma refeicao
     */
    public DayClosing(LocalDate date, UUID mealId, Status status, boolean partialNoService, int lunchMeals,
                      int dinnerMeals, int lunchDeliveries, int dinnerDeliveries, int sodas, BigDecimal price,
                      List<UUID> adjustmentIds, String note, List<DayClosing> meals) {
        this.date = date;
        this.mealId = mealId;
        this.status = status;
        this.partialNoService = partialNoService;
        this.lunchMeals = lunchMeals;
        this.dinnerMeals = dinnerMeals;
        this.lunchDeliveries = lunchDeliveries;
        this.dinnerDeliveries = dinnerDeliveries;
        this.sodas = sodas;
        this.price = price;
        this.adjustmentIds = List.copyOf(adjustmentIds);
        this.note = note;
        this.meals = List.copyOf(meals);
    }

    public static DayClosing empty(LocalDate date, UUID mealId, Status status) {
        return new DayClosing(date, mealId, status, false, 0, 0, 0, 0, 0, BigDecimal.ZERO, List.of(), null,
                List.of());
    }

    public static DayClosing empty(LocalDate date, Status status) {
        return empty(date, null, status);
    }

    /** Dia que fornece algo: fornecimento normal, dia alterado ou dia extra. */
    public boolean isServiceDay() {
        return status == Status.SERVICE || status == Status.CHANGED || status == Status.EXTRA_DAY;
    }

    public LocalDate date() {
        return date;
    }

    public UUID mealId() {
        return mealId;
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

    public int lunchDeliveries() {
        return lunchDeliveries;
    }

    public int dinnerDeliveries() {
        return dinnerDeliveries;
    }

    /** Entregas do almoco mais as do jantar. */
    public int deliveries() {
        return lunchDeliveries + dinnerDeliveries;
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

    public String note() {
        return note;
    }

    public List<DayClosing> meals() {
        return meals;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DayClosing other
                && Objects.equals(date, other.date)
                && Objects.equals(mealId, other.mealId)
                && status == other.status
                && partialNoService == other.partialNoService
                && lunchMeals == other.lunchMeals
                && dinnerMeals == other.dinnerMeals
                && lunchDeliveries == other.lunchDeliveries
                && dinnerDeliveries == other.dinnerDeliveries
                && sodas == other.sodas
                && Objects.equals(price, other.price)
                && adjustmentIds.equals(other.adjustmentIds)
                && Objects.equals(note, other.note)
                && meals.equals(other.meals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, mealId, status, partialNoService, lunchMeals, dinnerMeals, lunchDeliveries,
                dinnerDeliveries, sodas, price, adjustmentIds, note, meals);
    }

    @Override
    public String toString() {
        return "DayClosing[date=" + date + ", mealId=" + mealId + ", status=" + status + ", partialNoService="
                + partialNoService + ", lunchMeals=" + lunchMeals + ", dinnerMeals=" + dinnerMeals
                + ", lunchDeliveries=" + lunchDeliveries + ", dinnerDeliveries=" + dinnerDeliveries + ", sodas="
                + sodas + ", price=" + price + ", adjustmentIds=" + adjustmentIds + ", note=" + note + ", meals="
                + meals + "]";
    }
}
