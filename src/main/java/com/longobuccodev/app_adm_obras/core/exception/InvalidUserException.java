package com.longobuccodev.app_adm_obras.core.exception;

public class InvalidUserException extends CoreDomainException {

    private InvalidUserException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidUserException blankName() {
        return new InvalidUserException("user.invalid.name.blank", "User name must not be blank");
    }

    public static InvalidUserException shortName(String name) {
        return new InvalidUserException(
                "user.invalid.name.length",
                "User name must be at least 3 characters long: " + name
        );
    }

    public static InvalidUserException longName(String name) {
        return new InvalidUserException(
                "user.invalid.name.length",
                "User name must not exceed 100 characters: " + name
        );
    }

    public static InvalidUserException invalidEmail(String email) {
        return new InvalidUserException("user.invalid.email", "User email is invalid: " + email);
    }

    public static InvalidUserException missingRole() {
        return new InvalidUserException("user.invalid.role", "User role must not be null");
    }
}
