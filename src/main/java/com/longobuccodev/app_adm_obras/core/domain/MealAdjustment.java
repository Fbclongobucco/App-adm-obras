package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidMealAdjustmentException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Excecao de fornecimento de uma refeicao numa data: dia extra (sabado ou domingo), dia alterado (dia util com
 * outros numeros) ou sem fornecimento (dia util). No dia extra e no dia alterado, entregas e refrigerantes sao
 * opcionais: vazio usa o padrao (1 entrega por refeicao com quantidade, 1 refrigerante por entrega quando o
 * restaurante fornece). Incluido so por {@link Meal#addAdjustment} ou {@link Meal#putAdjustment}, que aplicam
 * as regras que dependem da refeicao e da obra. Valores calculados a partir da refeicao atual.
 */
public class MealAdjustment extends Audited {

    public static final int MAX_NOTE_LENGTH = 100;

    private final UUID id;
    private final LocalDate date;
    private final Type type;
    private final Integer lunchQuantity;
    private final Integer dinnerQuantity;
    private final Integer lunchDeliveries;
    private final Integer dinnerDeliveries;
    private final Integer sodas;
    private final String note;

    /** Ajuste com entregas e refrigerantes no padrao. */
    public MealAdjustment(UUID id, LocalDate date, Type type, Integer lunchQuantity, Integer dinnerQuantity,
                          String note) {
        this(id, date, type, lunchQuantity, dinnerQuantity, null, null, null, note);
    }

    public MealAdjustment(UUID id, LocalDate date, Type type, Integer lunchQuantity, Integer dinnerQuantity,
                          Integer lunchDeliveries, Integer dinnerDeliveries, Integer sodas, String note) {
        if (type == null) {
            throw InvalidMealAdjustmentException.missingType();
        }
        this.id = id == null ? UUID.randomUUID() : id;
        this.date = date;
        this.type = type;
        this.lunchQuantity = lunchQuantity;
        this.dinnerQuantity = dinnerQuantity;
        this.lunchDeliveries = lunchDeliveries;
        this.dinnerDeliveries = dinnerDeliveries;
        this.sodas = sodas;
        this.note = validateNote(note);
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public DayOfWeek getWeekday() {
        return date == null ? null : date.getDayOfWeek();
    }

    public Type getType() {
        return type;
    }

    public Integer getLunchQuantity() {
        return lunchQuantity;
    }

    public Integer getDinnerQuantity() {
        return dinnerQuantity;
    }

    /** Entregas do almoco informadas; {@code null} = padrao. */
    public Integer getLunchDeliveries() {
        return lunchDeliveries;
    }

    /** Entregas do jantar informadas; {@code null} = padrao. */
    public Integer getDinnerDeliveries() {
        return dinnerDeliveries;
    }

    /** Refrigerantes informados; {@code null} = padrao. */
    public Integer getSodas() {
        return sodas;
    }

    public String getNote() {
        return note;
    }

    /** Ajuste com quantidades no dia: dia extra ou dia alterado. */
    public boolean hasQuantities() {
        return type != Type.NO_SERVICE;
    }

    /** Almocos do dia pelo tipo atual da refeicao; 0 sem fornecimento. */
    public int lunchMeals(Meal meal) {
        return hasQuantities() && meal.getMealType().servesLunch() && lunchQuantity != null ? lunchQuantity : 0;
    }

    /** Jantares do dia pelo tipo atual da refeicao; 0 sem fornecimento. */
    public int dinnerMeals(Meal meal) {
        return hasQuantities() && meal.getMealType().servesDinner() && dinnerQuantity != null ? dinnerQuantity : 0;
    }

    /** Entregas do almoco informadas ou, no padrao, 1 quando ha almocos. */
    public int lunchDeliveries(Meal meal) {
        if (!hasQuantities() || !meal.getMealType().servesLunch()) {
            return 0;
        }
        return lunchDeliveries != null ? lunchDeliveries : lunchMeals(meal) > 0 ? 1 : 0;
    }

    /** Entregas do jantar informadas ou, no padrao, 1 quando ha jantares. */
    public int dinnerDeliveries(Meal meal) {
        if (!hasQuantities() || !meal.getMealType().servesDinner()) {
            return 0;
        }
        return dinnerDeliveries != null ? dinnerDeliveries : dinnerMeals(meal) > 0 ? 1 : 0;
    }

    /** Entregas do almoco mais as do jantar; 0 sem fornecimento. */
    public int deliveries(Meal meal) {
        return lunchDeliveries(meal) + dinnerDeliveries(meal);
    }

    /** Refrigerantes informados ou, no padrao, 1 por entrega; 0 quando o restaurante nao fornece. */
    public int sodas(Meal meal) {
        if (!hasQuantities() || meal.getSodaPrice() == null) {
            return 0;
        }
        return sodas != null ? sodas : deliveries(meal);
    }

    /**
     * Marmitas + entregas do almoco e do jantar x taxa de cada uma + refrigerantes x preco do restaurante;
     * zero sem fornecimento.
     */
    public BigDecimal price(Meal meal) {
        if (!hasQuantities()) {
            return BigDecimal.ZERO;
        }
        BigDecimal price = BigDecimal.ZERO;
        if (meal.getMealType().servesLunch()) {
            price = price.add(meal.getLunchPrice().multiply(BigDecimal.valueOf(lunchMeals(meal))));
        }
        if (meal.getMealType().servesDinner()) {
            price = price.add(meal.getDinnerPrice().multiply(BigDecimal.valueOf(dinnerMeals(meal))));
        }
        Restaurant restaurant = meal.getRestaurant();
        price = price.add(times(restaurant.getLunchDeliveryFee(), lunchDeliveries(meal)));
        price = price.add(times(restaurant.getDinnerDeliveryFee(), dinnerDeliveries(meal)));
        return price.add(times(meal.getSodaPrice(), sodas(meal)));
    }

    /** Copia com outros numeros (usada pela refeicao para descartar os de refeicao nao servida). */
    MealAdjustment withNumbers(Integer lunch, Integer dinner, Integer lunchDeliveries, Integer dinnerDeliveries,
                               Integer sodas) {
        MealAdjustment copy = new MealAdjustment(id, date, type, lunch, dinner, lunchDeliveries, dinnerDeliveries,
                sodas, note);
        copy.restoreAudit(getAudit());
        return copy;
    }

    /** Copia com outro id (usada ao substituir o ajuste de uma data mantendo o id existente). */
    MealAdjustment withId(UUID newId) {
        return new MealAdjustment(newId, date, type, lunchQuantity, dinnerQuantity, lunchDeliveries,
                dinnerDeliveries, sodas, note);
    }

    private static BigDecimal times(BigDecimal unitPrice, int count) {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(count));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof MealAdjustment adjustment && Objects.equals(id, adjustment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static String validateNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String normalized = note.strip();
        if (normalized.length() > MAX_NOTE_LENGTH) {
            throw InvalidMealAdjustmentException.invalidNote(normalized);
        }
        return normalized;
    }

    public enum Type {
        /** Sabado ou domingo com fornecimento: soma ao total. */
        EXTRA_DAY,
        /** Dia util com outros numeros: substitui o custo por dia nesse dia. */
        CHANGED,
        /** Dia util sem fornecimento: desconta um dia util. */
        NO_SERVICE
    }
}
