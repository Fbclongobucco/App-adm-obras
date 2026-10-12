package com.longobuccodev.app_adm_obras.core.exception;

public class InvalidRefreshTokenException extends CoreDomainException {

    private InvalidRefreshTokenException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidRefreshTokenException missingField(String field) {
        return new InvalidRefreshTokenException("refresh-token.invalid." + field,
                "Refresh token " + field + " must not be null");
    }

    public static InvalidRefreshTokenException expiryBeforeCreation() {
        return new InvalidRefreshTokenException("refresh-token.invalid.expiry",
                "Refresh token must expire after it is created");
    }
}
