package com.longobuccodev.app_adm_obras.core.exception;

import java.math.BigDecimal;

public class InvalidMealException extends CoreDomainException {

    private InvalidMealException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidMealException missingRestaurant() {
        return new InvalidMealException("meal.invalid.restaurant", "Meal must reference a restaurant");
    }

    public static InvalidMealException missingMealType() {
        return new InvalidMealException("meal.invalid.type", "Meal type must not be null");
    }

    public static InvalidMealException invalidLunchQuantity(Integer quantity) {
        return new InvalidMealException(
                "meal.invalid.lunch_quantity",
                "Lunch quantity must be between 1 and 10000: " + quantity
        );
    }

    public static InvalidMealException invalidLunchPrice(BigDecimal price) {
        return new InvalidMealException(
                "meal.invalid.lunch_price",
                "Lunch price must be greater than zero: " + price
        );
    }

    public static InvalidMealException invalidDinnerQuantity(Integer quantity) {
        return new InvalidMealException(
                "meal.invalid.dinner_quantity",
                "Dinner quantity must be between 1 and 10000: " + quantity
        );
    }

    public static InvalidMealException invalidDinnerPrice(BigDecimal price) {
        return new InvalidMealException(
                "meal.invalid.dinner_price",
                "Dinner price must be greater than zero: " + price
        );
    }
}
