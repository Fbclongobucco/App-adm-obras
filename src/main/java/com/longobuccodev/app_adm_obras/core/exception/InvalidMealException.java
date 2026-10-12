package com.longobuccodev.app_adm_obras.core.exception;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvalidMealException extends CoreDomainException {

    private InvalidMealException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidMealException missingRestaurant() {
        return new InvalidMealException("meal.invalid.restaurant", "Meal must reference a restaurant");
    }

    public static InvalidMealException invalidPrice(BigDecimal price) {
        return new InvalidMealException(
                "meal.invalid.price",
                "Meal price must be greater than zero: " + price
        );
    }

    public static InvalidMealException missingMealType() {
        return new InvalidMealException("meal.invalid.type", "Meal type must not be null");
    }

    public static InvalidMealException invalidQuantity(Integer quantity) {
        return new InvalidMealException(
                "meal.invalid.quantity",
                "Meal quantity must be between 1 and 10000: " + quantity
        );
    }

    public static InvalidMealException missingDate() {
        return new InvalidMealException("meal.invalid.date", "Meal date must not be null");
    }

    public static InvalidMealException futureDate(LocalDate date) {
        return new InvalidMealException(
                "meal.invalid.date.future",
                "Meal date must not be in the future: " + date
        );
    }
}