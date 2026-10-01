package com.longobuccodev.app_adm_obras.core.exception;

import java.math.BigDecimal;

public class InvalidAccommodationException extends CoreDomainException {

    private InvalidAccommodationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidAccommodationException blankHostName() {
        return new InvalidAccommodationException(
                "accommodation.invalid.host_name.blank",
                "Accommodation host name must not be blank"
        );
    }

    public static InvalidAccommodationException invalidHostName(String hostName) {
        return new InvalidAccommodationException(
                "accommodation.invalid.host_name.length",
                "Accommodation host name must be between 3 and 100 characters: " + hostName
        );
    }

    public static InvalidAccommodationException invalidHostPhone(String hostPhone) {
        return new InvalidAccommodationException(
                "accommodation.invalid.host_phone",
                "Accommodation host phone must have 10 or 11 digits: " + hostPhone
        );
    }

    public static InvalidAccommodationException missingAddress() {
        return new InvalidAccommodationException(
                "accommodation.invalid.address",
                "Accommodation address must not be null"
        );
    }

    public static InvalidAccommodationException invalidCapacity(Integer capacity) {
        return new InvalidAccommodationException(
                "accommodation.invalid.capacity",
                "Accommodation capacity must be between 1 and 1000: " + capacity
        );
    }

    public static InvalidAccommodationException invalidDays(Integer days) {
        return new InvalidAccommodationException(
                "accommodation.invalid.days",
                "Accommodation days must be between 1 and 365: " + days
        );
    }

    public static InvalidAccommodationException invalidTotalPrice(BigDecimal totalPrice) {
        return new InvalidAccommodationException(
                "accommodation.invalid.total_price",
                "Accommodation total price must not be negative: " + totalPrice
        );
    }
}