package com.longobuccodev.app_adm_obras.core.exception;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public static InvalidAccommodationException missingEmployee() {
        return new InvalidAccommodationException(
                "accommodation.invalid.employee",
                "Accommodation employee must not be null"
        );
    }

    public static InvalidAccommodationException invalidStartDate() {
        return new InvalidAccommodationException(
                "accommodation.invalid.start_date",
                "Accommodation start date must not be null"
        );
    }

    public static InvalidAccommodationException invalidMonthlyPrice(BigDecimal monthlyPrice) {
        return new InvalidAccommodationException(
                "accommodation.invalid.monthly_price",
                "Accommodation monthly price must be greater than zero: " + monthlyPrice
        );
    }

    public static InvalidAccommodationException invalidRenewalEnd(LocalDate start, LocalDate end) {
        return new InvalidAccommodationException(
                "accommodation.invalid.renewal_end",
                "Renewal end date must be on or after " + start + ": " + end
        );
    }

    public static InvalidAccommodationException invalidRenewalPrice(BigDecimal price) {
        return new InvalidAccommodationException(
                "accommodation.invalid.renewal_price",
                "Renewal price must be greater than zero: " + price
        );
    }
}
