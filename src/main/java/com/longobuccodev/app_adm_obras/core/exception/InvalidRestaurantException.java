package com.longobuccodev.app_adm_obras.core.exception;

import java.math.BigDecimal;

public class InvalidRestaurantException extends CoreDomainException {

    private InvalidRestaurantException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidRestaurantException invalidName(String name) {
        return new InvalidRestaurantException("restaurant.invalid.name",
                "Restaurant name must be between 2 and 100 characters: " + name);
    }

    public static InvalidRestaurantException invalidPhone(String phone) {
        return new InvalidRestaurantException("restaurant.invalid.phone",
                "Restaurant phone must have 10 or 11 digits: " + phone);
    }

    public static InvalidRestaurantException missingProject() {
        return new InvalidRestaurantException("restaurant.invalid.project", "Restaurant must belong to a project");
    }

    public static InvalidRestaurantException invalidDeliveryFee(BigDecimal deliveryFee) {
        return new InvalidRestaurantException("restaurant.invalid.delivery_fee",
                "Restaurant delivery fee must be greater than zero: " + deliveryFee);
    }

    public static InvalidRestaurantException invalidSodaPrice(BigDecimal sodaPrice) {
        return new InvalidRestaurantException("restaurant.invalid.soda_price",
                "Restaurant soda price must be greater than zero: " + sodaPrice);
    }
}
