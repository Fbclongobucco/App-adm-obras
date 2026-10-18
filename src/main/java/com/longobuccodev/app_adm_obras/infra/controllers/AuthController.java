package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.auth.AuthResult;
import com.longobuccodev.app_adm_obras.application.dto.auth.AuthTokenResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginOrigin;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.AuthUseCase;
import com.longobuccodev.app_adm_obras.core.exception.AuthenticationFailedException;
import com.longobuccodev.app_adm_obras.infra.security.RefreshCookieProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthUseCase authUseCase;
    private final RefreshCookieProperties cookieProperties;
    private final Clock clock;

    public AuthController(AuthUseCase authUseCase, RefreshCookieProperties cookieProperties, Clock clock) {
        this.authUseCase = authUseCase;
        this.cookieProperties = cookieProperties;
        this.clock = clock;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponseDTO> login(@RequestBody(required = false) LoginRequestDTO dto,
                                                      HttpServletRequest request) {
        // Atras de proxy, o IP do cliente so aparece com server.forward-headers-strategy=native
        LoginOrigin origin = new LoginOrigin(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(authUseCase.login(dto, origin));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponseDTO> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse servletResponse) {
        try {
            return withRefreshCookie(authUseCase.refresh(refreshToken));
        } catch (AuthenticationFailedException ex) {
            // O 401 sai pelo GlobalExceptionHandler; o cookie invalido e apagado junto
            servletResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie().toString());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authUseCase.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponseDTO> me(Authentication authentication) {
        return ResponseEntity.ok(authUseCase.profile(UUID.fromString(authentication.getName())));
    }

    private ResponseEntity<AuthTokenResponseDTO> withRefreshCookie(AuthResult result) {
        Duration maxAge = Duration.between(clock.instant(), result.refreshTokenExpiresAt());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(result.refreshToken(), maxAge).toString())
                .body(result.response());
    }

    private ResponseCookie clearCookie() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        // Sem Domain: o cookie fica preso ao host que o emitiu
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .partitioned(cookieProperties.partitioned())
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }
}
