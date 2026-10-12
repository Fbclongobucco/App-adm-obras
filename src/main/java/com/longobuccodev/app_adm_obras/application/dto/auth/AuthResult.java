package com.longobuccodev.app_adm_obras.application.dto.auth;

import java.time.Instant;

/**
 * Resultado interno de login e refresh. Nunca e serializado: o controller
 * devolve {@link #response()} no corpo e usa o refresh token so para o cookie.
 */
public record AuthResult(
        AuthTokenResponseDTO response,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {

    @Override
    public String toString() {
        return "AuthResult[response=" + response + ", refreshToken=***, refreshTokenExpiresAt="
                + refreshTokenExpiresAt + "]";
    }
}
