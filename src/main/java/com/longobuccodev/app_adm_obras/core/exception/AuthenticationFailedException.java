package com.longobuccodev.app_adm_obras.core.exception;

public class AuthenticationFailedException extends CoreDomainException {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private AuthenticationFailedException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("auth.invalid.credentials", INVALID_CREDENTIALS_MESSAGE);
    }

    /** Mesma resposta de senha incorreta, sem o e-mail, para nao revelar se ele existe. */
    public static AuthenticationFailedException unknownEmail(String email) {
        return invalidCredentials();
    }

    public static AuthenticationFailedException invalidRefreshToken() {
        return new AuthenticationFailedException("auth.invalid.refresh", "Invalid or expired refresh token");
    }
}
