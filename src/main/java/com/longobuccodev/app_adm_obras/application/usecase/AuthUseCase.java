package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.auth.AuthResult;
import com.longobuccodev.app_adm_obras.application.dto.auth.AuthTokenResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginOrigin;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginResponseDTO;
import com.longobuccodev.app_adm_obras.application.security.RefreshTokenFactory;
import com.longobuccodev.app_adm_obras.core.domain.LoginRecord;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.exception.AuthenticationFailedException;
import com.longobuccodev.app_adm_obras.core.repository.LoginAuditRepository;
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
    private final LoginAuditRepository loginAuditRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenFactory refreshTokenFactory;
    private final Duration refreshTokenTtl;
    private final Duration refreshReuseGrace;
    private final Clock clock;

    public AuthUseCase(AuthenticationGateway authenticationGateway, UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository, LoginAuditRepository loginAuditRepository,
                       AccessTokenIssuer accessTokenIssuer,
                       RefreshTokenFactory refreshTokenFactory, Duration refreshTokenTtl,
                       Duration refreshReuseGrace, Clock clock) {
        this.authenticationGateway = authenticationGateway;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAuditRepository = loginAuditRepository;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenFactory = refreshTokenFactory;
        this.refreshTokenTtl = refreshTokenTtl;
        this.refreshReuseGrace = refreshReuseGrace;
        this.clock = clock;
    }

    /**
     * Autentica e abre a sessao. O login com sucesso e registrado antes da emissao dos tokens: se o registro
     * falhar, nenhuma sessao e emitida. Login rejeitado nao e registrado.
     */
    public AuthResult login(LoginRequestDTO dto, LoginOrigin origin) {
        if (dto == null || dto.email() == null || dto.email().isBlank()
                || dto.password() == null || dto.password().isBlank()) {
            throw AuthenticationFailedException.invalidCredentials();
        }
        User user = authenticationGateway.authenticate(dto.email(), dto.password());
        Instant now = clock.instant();
        LoginOrigin from = origin == null ? LoginOrigin.UNKNOWN : origin;
        loginAuditRepository.save(LoginRecord.of(user, now, from.ipAddress(), from.userAgent()));
        String raw = refreshTokenFactory.newToken();
        RefreshToken token = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), refreshTokenFactory.hash(raw), now, now.plus(refreshTokenTtl)));
        return result(user, raw, token);
    }

    /**
     * Troca um refresh token por um novo par. O token apresentado vale uma
     * unica vez; reapresentar um token ja trocado revoga a familia inteira,
     * exceto dentro da janela de tolerancia (ver {@link #reuse}).
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
            return reuse(current, now);
        }
        if (!current.isActive(now)) {
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        User user = activeUser(current, now);
        if (!refreshTokenRepository.markRotatedIfActive(current.getId(), now)) {
            // Outra requisicao trocou o mesmo token antes desta: mesma regra do reuso
            return reuse(refreshTokenRepository.findByHash(current.getTokenHash()), now);
        }
        return issueSuccessor(current, user, now);
    }

    /**
     * Token ja trocado reapresentado. Clientes com varias requisicoes simultaneas
     * (abas, instancias do front) apresentam o mesmo token mais de uma vez em
     * poucos segundos; dentro da janela, com a sessao aberta, recebem outro
     * sucessor da mesma familia. Fora dela o reuso e tratado como roubo.
     */
    private AuthResult reuse(RefreshToken rotated, Instant now) {
        if (rotated == null || !rotated.wasRotated() || rotated.isExpired(now)
                || refreshTokenRepository.isFamilyClosed(rotated.getFamilyId())) {
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        Duration sinceRotation = Duration.between(rotated.getRevokedAt(), now);
        if (refreshReuseGrace.isZero() || sinceRotation.compareTo(refreshReuseGrace) > 0) {
            refreshTokenRepository.revokeFamily(rotated.getFamilyId(), RevokeReason.REUSE, now);
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        return issueSuccessor(rotated, activeUser(rotated, now), now);
    }

    /** Usuario dono do token; ausente ou inativo revoga a familia. */
    private User activeUser(RefreshToken token, Instant now) {
        User user = userRepository.findById(token.getUserId());
        if (user == null || !user.isActive()) {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), RevokeReason.USER_CHANGED, now);
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        return user;
    }

    private AuthResult issueSuccessor(RefreshToken previous, User user, Instant now) {
        String raw = refreshTokenFactory.newToken();
        RefreshToken next = refreshTokenRepository.save(previous.successor(refreshTokenFactory.hash(raw), now));
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
