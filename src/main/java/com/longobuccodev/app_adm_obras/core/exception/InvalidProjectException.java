package com.longobuccodev.app_adm_obras.core.exception;

import java.time.LocalDate;

public class InvalidProjectException extends CoreDomainException {

    private InvalidProjectException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidProjectException blankOs() {
        return new InvalidProjectException("project.invalid.os.blank", "Project OS must not be blank");
    }

    public static InvalidProjectException invalidOs(String os) {
        return new InvalidProjectException(
                "project.invalid.os",
                "Project OS must have 3 to 30 letters, digits, dots, slashes, hyphens or underscores: " + os
        );
    }

    public static InvalidProjectException invalidDescription(String description) {
        return new InvalidProjectException(
                "project.invalid.description.length",
                "Project description must be between 3 and 500 characters: " + description
        );
    }

    public static InvalidProjectException missingStartDate() {
        return new InvalidProjectException("project.invalid.start_date", "Project start date must not be null");
    }

    public static InvalidProjectException invalidEndDate(LocalDate startDate, LocalDate endDate) {
        return new InvalidProjectException(
                "project.invalid.end_date",
                "Project end date must not be before the start date " + startDate + ": " + endDate
        );
    }

    public static InvalidProjectException missingClient() {
        return new InvalidProjectException("project.invalid.client", "Project client must not be null");
    }

    public static InvalidProjectException missingMeal() {
        return new InvalidProjectException("project.invalid.meal", "Project meal must not be null");
    }

    public static InvalidProjectException duplicatedMealType(String mealType) {
        return new InvalidProjectException(
                "project.duplicated_meal_type",
                "Project already has a " + mealType + " meal registered"
        );
    }

    public static InvalidProjectException missingMealType(String mealType) {
        return new InvalidProjectException(
                "project.missing_meal_type",
                "Project has no " + mealType + " meal registered"
        );
    }

    public static InvalidProjectException mealTypeMismatch(String expectedType, String actualType) {
        return new InvalidProjectException(
                "project.invalid.meal_type",
                "Meal type " + actualType + " cannot be assigned to the " + expectedType + " property"
        );
    }
}