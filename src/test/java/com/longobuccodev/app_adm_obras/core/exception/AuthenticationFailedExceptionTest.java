package com.longobuccodev.app_adm_obras.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFailedExceptionTest {

    @Test
    void unknownEmailShouldBeIndistinguishableFromWrongPassword() {
        AuthenticationFailedException unknown = AuthenticationFailedException.unknownEmail("ninguem@email.com");
        AuthenticationFailedException wrong = AuthenticationFailedException.invalidCredentials();

        assertThat(unknown.getErrorCode()).isEqualTo(wrong.getErrorCode());
        assertThat(unknown.getMessage()).isEqualTo(wrong.getMessage());
        assertThat(unknown.getMessage()).doesNotContain("ninguem@email.com");
    }

    @Test
    void invalidRefreshTokenShouldHaveOwnCode() {
        assertThat(AuthenticationFailedException.invalidRefreshToken().getErrorCode())
                .isEqualTo("auth.invalid.refresh");
    }
}
