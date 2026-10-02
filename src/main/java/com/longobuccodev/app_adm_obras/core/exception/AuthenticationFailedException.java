package com.longobuccodev.app_adm_obras.core.exception;

public class AuthenticationFailedException extends CoreDomainException {

    private AuthenticationFailedException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("auth.invalid.credentials", "Invalid email or password");
    }

    public static AuthenticationFailedException unknownEmail(String email) {
        return new AuthenticationFailedException("auth.invalid.credentials", "Invalid email or password: " + email);
    }
}
