package com.longobuccodev.app_adm_obras.core.exception;

public class InvalidAddressException extends CoreDomainException {

    private InvalidAddressException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidAddressException missingStreet() {
        return new InvalidAddressException("address.invalid.street", "Address street must not be blank");
    }

    public static InvalidAddressException missingNumber() {
        return new InvalidAddressException("address.invalid.number", "Address number must not be blank");
    }

    public static InvalidAddressException missingNeighborhood() {
        return new InvalidAddressException("address.invalid.neighborhood", "Address neighborhood must not be blank");
    }

    public static InvalidAddressException missingCity() {
        return new InvalidAddressException("address.invalid.city", "Address city must not be blank");
    }

    public static InvalidAddressException missingState() {
        return new InvalidAddressException("address.invalid.state.blank", "Address state must not be blank");
    }

    public static InvalidAddressException invalidState(String state) {
        return new InvalidAddressException(
                "address.invalid.state",
                "Address state must be a 2 letter Brazilian federation unit: " + state
        );
    }

    public static InvalidAddressException missingCountry() {
        return new InvalidAddressException("address.invalid.country", "Address country must not be blank");
    }

    public static InvalidAddressException invalidZipCode(String zipCode) {
        return new InvalidAddressException(
                "address.invalid.zip_code",
                "Address zip code must have 8 digits: " + zipCode
        );
    }
}