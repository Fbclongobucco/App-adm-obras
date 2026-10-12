package com.longobuccodev.app_adm_obras.application.dto.auth;

/**
 * Corpo de login e refresh. O refresh token nao faz parte da resposta: vai
 * apenas no cookie.
 */
public record AuthTokenResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        LoginResponseDTO user
) {
}
