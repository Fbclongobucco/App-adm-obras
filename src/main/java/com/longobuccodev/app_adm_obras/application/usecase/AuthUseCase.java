package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.auth.AuthResult;
import com.longobuccodev.app_adm_obras.application.dto.auth.AuthTokenResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginResponseDTO;
import com.longobuccodev.app_adm_obras.application.security.RefreshTokenFactory;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.exception.AuthenticationFailedException;
import com.longobuccodev.app_adm_obras.core.repository.RefreshTokenRepository;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.AccessTokenIssuer;
import com.longobuccodev.app_adm_obras.core.security.AccessTokenIssuer.IssuedAccessToken;
import com.longobuccodev.app_adm_obras.core.security.AuthenticationGateway;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class AuthUseCase {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationGateway authenticationGateway;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenFactory refreshTokenFactory;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    public AuthUseCase(AuthenticationGateway authenticationGateway, UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository, AccessTokenIssuer accessTokenIssuer,
                       RefreshTokenFactory refreshTokenFactory, Duration refreshTokenTtl, Clock clock) {
        this.authenticationGateway = authenticationGateway;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenFactory = refreshTokenFactory;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    public AuthResult login(LoginRequestDTO dto) {
        if (dto == null || dto.email() == null || dto.email().isBlank()
                || dto.password() == null || dto.password().isBlank()) {
            throw AuthenticationFailedException.invalidCredentials();
        }
        User user = authenticationGateway.authenticate(dto.email(), dto.password());
        Instant now = clock.instant();
        String raw = refreshTokenFactory.newToken();
        RefreshToken token = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), refreshTokenFactory.hash(raw), now, now.plus(refreshTokenTtl)));
        return result(user, raw, token);
    }

    /**
     * Troca um refresh token por um novo par. O token apresentado vale uma
     * unica vez; reapresentar um token ja trocado revoga a familia inteira.
     */
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByHash(refreshTokenFactory.hash(rawToken));
        if (current == null) {
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        if (current.wasRotated()) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), RevokeReason.REUSE, now);
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        if (!current.isActive(now)) {
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        User user = userRepository.findById(current.getUserId());
        if (user == null || !user.isActive()) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), RevokeReason.USER_CHANGED, now);
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        if (!refreshTokenRepository.markRotatedIfActive(current.getId(), now)) {
            // Outra requisicao trocou o mesmo token antes desta: tratado como reuso
            refreshTokenRepository.revokeFamily(current.getFamilyId(), RevokeReason.REUSE, now);
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        String raw = refreshTokenFactory.newToken();
        RefreshToken next = refreshTokenRepository.save(current.successor(refreshTokenFactory.hash(raw), now));
        return result(user, raw, next);
    }

    /** Revoga a sessao do refresh token informado. Silencioso para token ausente ou invalido. */
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        RefreshToken current = refreshTokenRepository.findByHash(refreshTokenFactory.hash(rawToken));
        if (current != null && !current.isRevoked()) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), RevokeReason.LOGOUT, clock.instant());
        }
    }

    public LoginResponseDTO profile(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw AuthenticationFailedException.invalidCredentials();
        }
        return toProfile(user);
    }

    private AuthResult result(User user, String rawRefreshToken, RefreshToken refreshToken) {
        IssuedAccessToken access = accessTokenIssuer.issue(user);
        long expiresIn = Duration.between(access.issuedAt(), access.expiresAt()).toSeconds();
        AuthTokenResponseDTO response = new AuthTokenResponseDTO(access.value(), TOKEN_TYPE, expiresIn, toProfile(user));
        return new AuthResult(response, rawRefreshToken, refreshToken.getExpiresAt());
    }

    private LoginResponseDTO toProfile(User user) {
        return new LoginResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRoles());
    }
}
