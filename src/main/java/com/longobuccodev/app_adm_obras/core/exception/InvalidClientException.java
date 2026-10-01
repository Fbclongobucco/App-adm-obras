package com.longobuccodev.app_adm_obras.core.exception;

public class InvalidClientException extends CoreDomainException {

    private InvalidClientException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidClientException blankName() {
        return new InvalidClientException("client.invalid.name.blank", "Client name must not be blank");
    }

    public static InvalidClientException shortName(String name) {
        return new InvalidClientException(
                "client.invalid.name.length",
                "Client name must be at least 3 characters long: " + name
        );
    }

    public static InvalidClientException longName(String name) {
        return new InvalidClientException(
                "client.invalid.name.length",
                "Client name must not exceed 100 characters: " + name
        );
    }

    public static InvalidClientException invalidEmail(String email) {
        return new InvalidClientException("client.invalid.email", "Client email is invalid: " + email);
    }

    public static InvalidClientException invalidPhone(String phone) {
        return new InvalidClientException(
                "client.invalid.phone",
                "Client phone must have 10 or 11 digits: " + phone
        );
    }

    public static InvalidClientException missingAddress() {
        return new InvalidClientException("client.invalid.address", "Client address must not be null");
    }
}