package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidMealAdjustmentException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Excecao de fornecimento de uma refeicao numa data: dia extra (sabado ou domingo, com as quantidades do
 * dia) ou sem fornecimento (dia util). Incluido so por {@link Meal#addAdjustment}, que aplica as regras
 * que dependem da refeicao e da obra. Valores calculados a partir da refeicao atual.
 */
public class MealAdjustment extends Audited {

    public static final int MAX_NOTE_LENGTH = 100;

    private final UUID id;
    private final LocalDate date;
    private final Type type;
    private final Integer lunchQuantity;
    private final Integer dinnerQuantity;
    private final String note;

    public MealAdjustment(UUID id, LocalDate date, Type type, Integer lunchQuantity, Integer dinnerQuantity,
                          String note) {
        if (type == null) {
            throw InvalidMealAdjustmentException.missingType();
        }
        this.id = id == null ? UUID.randomUUID() : id;
        this.date = date;
        this.type = type;
        this.lunchQuantity = lunchQuantity;
        this.dinnerQuantity = dinnerQuantity;
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

    public String getNote() {
        return note;
    }

    /** Uma entrega por refeicao servida pelo tipo atual da refeicao com quantidade maior que zero; 0 sem fornecimento. */
    public int deliveries(Meal meal) {
        if (type != Type.EXTRA_DAY) {
            return 0;
        }
        return (servesLunch(meal) ? 1 : 0) + (servesDinner(meal) ? 1 : 0);
    }

    /** Um refrigerante por entrega quando o restaurante fornece; 0 caso contrario. */
    public int sodas(Meal meal) {
        return meal.getSodaPrice() == null ? 0 : deliveries(meal);
    }

    /** Marmitas + entregas x taxa do restaurante + refrigerantes x preco do restaurante; zero sem fornecimento. */
    public BigDecimal price(Meal meal) {
        BigDecimal price = BigDecimal.ZERO;
        if (servesLunch(meal)) {
            price = price.add(meal.getLunchPrice().multiply(BigDecimal.valueOf(lunchQuantity)));
        }
        if (servesDinner(meal)) {
            price = price.add(meal.getDinnerPrice().multiply(BigDecimal.valueOf(dinnerQuantity)));
        }
        int deliveries = deliveries(meal);
        BigDecimal fee = meal.getRestaurant().getDeliveryFee();
        if (fee != null) {
            price = price.add(fee.multiply(BigDecimal.valueOf(deliveries)));
        }
        if (meal.getSodaPrice() != null) {
            price = price.add(meal.getSodaPrice().multiply(BigDecimal.valueOf(sodas(meal))));
        }
        return price;
    }

    /** Copia com outras quantidades (usada pela refeicao para descartar a de refeicao nao servida). */
    MealAdjustment withQuantities(Integer lunch, Integer dinner) {
        MealAdjustment copy = new MealAdjustment(id, date, type, lunch, dinner, note);
        copy.restoreAudit(getAudit());
        return copy;
    }

    private boolean servesLunch(Meal meal) {
        return type == Type.EXTRA_DAY && meal.getMealType().servesLunch() && lunchQuantity != null && lunchQuantity > 0;
    }

    private boolean servesDinner(Meal meal) {
        return type == Type.EXTRA_DAY && meal.getMealType().servesDinner() && dinnerQuantity != null
                && dinnerQuantity > 0;
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
        /** Dia util sem fornecimento: desconta um dia util. */
        NO_SERVICE
    }
}
