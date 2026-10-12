package com.longobuccodev.app_adm_obras.core.exception;

import java.time.LocalDate;

/** Ajuste por data da refeicao invalido (400). Data repetida e conflito: ver {@link ConflictException}. */
public class InvalidMealAdjustmentException extends CoreDomainException {

    private InvalidMealAdjustmentException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidMealAdjustmentException missingType() {
        return new InvalidMealAdjustmentException("meal.adjustment.invalid.type",
                "Adjustment type must be EXTRA_DAY or NO_SERVICE");
    }

    public static InvalidMealAdjustmentException invalidNote(String note) {
        return new InvalidMealAdjustmentException("meal.adjustment.invalid.note",
                "Adjustment note must have at most 100 characters: " + note);
    }

    public static InvalidMealAdjustmentException dateOutsideProject(LocalDate date, LocalDate start, LocalDate end) {
        return new InvalidMealAdjustmentException("meal.adjustment.invalid.date",
                "Adjustment date must be within the project period (" + start + " to "
                        + (end == null ? "open" : end) + "): " + date);
    }

    public static InvalidMealAdjustmentException extraDayOnWeekday(LocalDate date) {
        return new InvalidMealAdjustmentException("meal.adjustment.invalid.extra_day",
                "Extra day must be a Saturday or Sunday: " + date);
    }

    public static InvalidMealAdjustmentException noServiceOnWeekend(LocalDate date) {
        return new InvalidMealAdjustmentException("meal.adjustment.invalid.no_service",
                "No service must be a weekday (Monday to Friday): " + date);
    }

    public static InvalidMealAdjustmentException invalidQuantity(String detail) {
        return new InvalidMealAdjustmentException("meal.adjustment.invalid.quantity", detail);
    }

    public static ConflictException duplicateDate(LocalDate date) {
        return ConflictException.of("meal.adjustment.duplicate.date",
                "Meal already has an adjustment on " + date);
    }
}
