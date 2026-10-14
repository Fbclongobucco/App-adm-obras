package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.auth.AuthResult;
import com.longobuccodev.app_adm_obras.application.dto.auth.LoginRequestDTO;
import com.longobuccodev.app_adm_obras.application.security.RefreshTokenFactory;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.exception.AuthenticationFailedException;
import com.longobuccodev.app_adm_obras.core.repository.RefreshTokenRepository;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.AccessTokenIssuer;
import com.longobuccodev.app_adm_obras.core.security.AuthenticationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthUseCaseTest {

    private static final Instant LOGIN_TIME = Instant.parse("2026-10-11T12:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    private MutableClock clock;
    private InMemoryRefreshTokenRepository tokens;
    private UserRepository users;
    private User user;
    private AuthenticationGateway gateway;
    private AccessTokenIssuer issuer;
    private AuthUseCase useCase;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(LOGIN_TIME);
        tokens = new InMemoryRefreshTokenRepository();
        users = mock(UserRepository.class);
        user = new User(null, "Ana Souza", "ana@email.com", true, "$2a$10$abcdefghijklmnopqrstuv");
        user.addRole(Role.ADMIN);
        when(users.findById(user.getId())).thenAnswer(invocation -> user);

        gateway = mock(AuthenticationGateway.class);
        when(gateway.authenticate(any(), any())).thenAnswer(invocation -> {
            if ("ana@email.com".equals(invocation.getArgument(0)) && "senha123".equals(invocation.getArgument(1))) {
                return user;
            }
            throw AuthenticationFailedException.invalidCredentials();
        });

        // Emissor falso: o valor expoe as roles para os testes verificarem o que foi emitido
        issuer = u -> new AccessTokenIssuer.IssuedAccessToken(
                "token-" + u.getRoles(), clock.instant(), clock.instant().plus(Duration.ofMinutes(15)));

        useCase = useCaseWithGrace(REUSE_GRACE);
    }

    private AuthUseCase useCaseWithGrace(Duration grace) {
        return new AuthUseCase(gateway, users, tokens, issuer, new RefreshTokenFactory(), REFRESH_TTL, grace, clock);
    }

    private AuthResult login() {
        return useCase.login(new LoginRequestDTO("ana@email.com", "senha123"));
    }

    @Test
    @DisplayName("login deve emitir access token e abrir familia com expiracao de 7 dias")
    void loginShouldOpenFamily() {
        AuthResult result = login();

        assertThat(result.response().tokenType()).isEqualTo("Bearer");
        assertThat(result.response().expiresIn()).isEqualTo(900);
        assertThat(result.response().user().email()).isEqualTo("ana@email.com");
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(LOGIN_TIME.plus(REFRESH_TTL));
        assertThat(tokens.all()).hasSize(1);
        assertThat(tokens.all().getFirst().getTokenHash()).isNotEqualTo(result.refreshToken());
    }

    @Test
    @DisplayName("login com credenciais invalidas ou ausentes deve falhar sem criar token")
    void loginWithInvalidCredentials() {
        assertThatThrownBy(() -> useCase.login(new LoginRequestDTO("ana@email.com", "errada")))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> useCase.login(null)).isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> useCase.login(new LoginRequestDTO("ana@email.com", " ")))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.all()).isEmpty();
    }

    @Test
    @DisplayName("refresh deve rotacionar e herdar a expiracao absoluta")
    void refreshShouldRotateAndKeepExpiry() {
        AuthResult first = login();
        clock.advance(Duration.ofDays(3));

        AuthResult second = useCase.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(second.refreshTokenExpiresAt()).isEqualTo(first.refreshTokenExpiresAt());
        assertThat(tokens.byFamily(tokens.all().getFirst().getFamilyId())).hasSize(2);
        assertThat(tokens.all().getFirst().wasRotated()).isTrue();
    }

    @Test
    @DisplayName("refresh apos a expiracao do login deve falhar")
    void refreshAfterExpiryShouldFail() {
        AuthResult first = login();
        clock.advance(Duration.ofDays(3));
        AuthResult second = useCase.refresh(first.refreshToken());
        clock.advance(Duration.ofDays(4));

        assertThatThrownBy(() -> useCase.refresh(second.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class)
                .extracting("errorCode").isEqualTo("auth.invalid.refresh");
    }

    @Test
    @DisplayName("reapresentar token ja trocado apos a janela deve revogar a familia, inclusive o mais novo")
    void replayShouldRevokeFamily() {
        AuthResult first = login();
        AuthResult second = useCase.refresh(first.refreshToken());
        clock.advance(REUSE_GRACE.plusSeconds(1));

        assertThatThrownBy(() -> useCase.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> useCase.refresh(second.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.all()).allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("revogacao por reuso nao deve afetar outro login do mesmo usuario")
    void otherFamilyShouldSurviveReplay() {
        AuthResult sessionA = login();
        AuthResult sessionB = login();
        useCase.refresh(sessionA.refreshToken());
        clock.advance(REUSE_GRACE.plusSeconds(1));

        assertThatThrownBy(() -> useCase.refresh(sessionA.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(useCase.refresh(sessionB.refreshToken()).response().accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("reuso dentro da janela deve emitir novo par sem revogar a sessao")
    void replayWithinGraceShouldIssueSibling() {
        AuthResult first = login();
        AuthResult second = useCase.refresh(first.refreshToken());
        clock.advance(Duration.ofSeconds(2));

        AuthResult sibling = useCase.refresh(first.refreshToken());

        assertThat(sibling.refreshToken()).isNotEqualTo(second.refreshToken());
        assertThat(sibling.refreshTokenExpiresAt()).isEqualTo(first.refreshTokenExpiresAt());
        assertThat(tokens.all()).noneMatch(t -> t.getRevokeReason() == RevokeReason.REUSE);
        assertThat(useCase.refresh(second.refreshToken()).response().accessToken()).isNotBlank();
        assertThat(useCase.refresh(sibling.refreshToken()).response().accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("reuso no limite da janela ainda deve renovar")
    void replayAtGraceLimitShouldIssueSibling() {
        AuthResult first = login();
        useCase.refresh(first.refreshToken());
        clock.advance(REUSE_GRACE);

        assertThat(useCase.refresh(first.refreshToken()).refreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("disputa no mesmo instante deve renovar as duas requisicoes")
    void concurrentRotationShouldIssueSibling() {
        AuthResult first = login();
        // Outra requisicao troca o token entre a leitura e a marcacao desta
        tokens.beforeMarkRotated = token -> {
            token.revoke(RevokeReason.ROTATED, clock.instant());
            tokens.save(token.successor("hash-da-outra-requisicao", clock.instant()));
        };

        AuthResult result = useCase.refresh(first.refreshToken());

        assertThat(result.refreshToken()).isNotBlank();
        assertThat(tokens.byFamily(tokens.all().getFirst().getFamilyId())).hasSize(3);
        assertThat(tokens.all()).noneMatch(t -> t.getRevokeReason() == RevokeReason.REUSE);
    }

    @Test
    @DisplayName("janela zero deve revogar a familia em qualquer reuso")
    void zeroGraceShouldRevokeOnAnyReplay() {
        useCase = useCaseWithGrace(Duration.ZERO);
        AuthResult first = login();
        AuthResult second = useCase.refresh(first.refreshToken());
        clock.advance(Duration.ofSeconds(1));

        assertThatThrownBy(() -> useCase.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> useCase.refresh(second.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("reuso dentro da janela apos logout deve falhar sem emitir token")
    void replayAfterLogoutShouldFail() {
        AuthResult first = login();
        AuthResult second = useCase.refresh(first.refreshToken());
        useCase.logout(second.refreshToken());
        int before = tokens.all().size();

        assertThatThrownBy(() -> useCase.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.all()).hasSize(before);
    }

    @Test
    @DisplayName("reuso dentro da janela apos a expiracao da sessao deve falhar sem emitir token")
    void replayAfterExpiryShouldFail() {
        AuthResult first = login();
        clock.advance(REFRESH_TTL.minusSeconds(5));
        useCase.refresh(first.refreshToken());
        clock.advance(Duration.ofSeconds(10));
        int before = tokens.all().size();

        assertThatThrownBy(() -> useCase.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.all()).hasSize(before);
    }

    @Test
    @DisplayName("reuso dentro da janela com usuario inativo deve falhar e revogar a familia")
    void replayWithinGraceWithInactiveUserShouldFail() {
        AuthResult first = login();
        useCase.refresh(first.refreshToken());
        user.setActive(false);

        assertThatThrownBy(() -> useCase.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.all()).allMatch(RefreshToken::isRevoked);
        assertThat(tokens.all()).anyMatch(t -> t.getRevokeReason() == RevokeReason.USER_CHANGED);
    }

    @Test
    @DisplayName("refresh de usuario inativo deve falhar e revogar a familia")
    void inactiveUserShouldBeRefused() {
        AuthResult first = login();
        user.setActive(false);

        assertThatThrownBy(() -> useCase.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.all()).allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("refresh deve emitir access token com as roles atuais")
    void refreshShouldReflectCurrentRoles() {
        AuthResult first = login();
        user.removeRole(Role.ADMIN);
        user.addRole(Role.OPERADOR);

        AuthResult second = useCase.refresh(first.refreshToken());

        assertThat(second.response().accessToken()).contains("OPERADOR").doesNotContain("ADMIN");
    }

    @Test
    @DisplayName("refresh sem token ou com token desconhecido deve falhar")
    void unknownTokenShouldFail() {
        assertThatThrownBy(() -> useCase.refresh(null)).isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> useCase.refresh("nunca-emitido")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("o hash armazenado nao deve servir como token")
    void storedHashShouldNotWorkAsToken() {
        login();
        String storedHash = tokens.all().getFirst().getTokenHash();

        assertThatThrownBy(() -> useCase.refresh(storedHash)).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("logout deve revogar o token e impedir o refresh")
    void logoutShouldRevoke() {
        AuthResult first = login();

        useCase.logout(first.refreshToken());

        assertThat(tokens.all().getFirst().getRevokeReason()).isEqualTo(RevokeReason.LOGOUT);
        assertThatThrownBy(() -> useCase.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("logout com token desconhecido ou ausente nao deve lancar excecao")
    void logoutWithUnknownTokenIsSilent() {
        assertThatCode(() -> useCase.logout("desconhecido")).doesNotThrowAnyException();
        assertThatCode(() -> useCase.logout(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil deve ser buscado pelo id")
    void profileById() {
        assertThat(useCase.profile(user.getId()).email()).isEqualTo("ana@email.com");
        assertThatThrownBy(() -> useCase.profile(UUID.randomUUID()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** Repositorio em memoria com a mesma semantica de revogacao do adaptador JPA. */
    static final class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

        private final List<RefreshToken> tokens = new ArrayList<>();
        /** Simula outra requisicao agindo entre a leitura e a marcacao do token. */
        Consumer<RefreshToken> beforeMarkRotated = token -> { };

        List<RefreshToken> all() {
            return tokens;
        }

        List<RefreshToken> byFamily(UUID familyId) {
            return tokens.stream().filter(t -> t.getFamilyId().equals(familyId)).toList();
        }

        @Override
        public RefreshToken save(RefreshToken token) {
            tokens.add(token);
            return token;
        }

        @Override
        public RefreshToken findByHash(String tokenHash) {
            return tokens.stream().filter(t -> t.getTokenHash().equals(tokenHash)).findFirst().orElse(null);
        }

        @Override
        public boolean markRotatedIfActive(UUID id, Instant now) {
            RefreshToken token = tokens.stream().filter(t -> t.getId().equals(id)).findFirst().orElseThrow();
            beforeMarkRotated.accept(token);
            if (token.isRevoked()) {
                return false;
            }
            token.revoke(RevokeReason.ROTATED, now);
            return true;
        }

        @Override
        public boolean isFamilyClosed(UUID familyId) {
            return byFamily(familyId).stream()
                    .anyMatch(t -> t.isRevoked() && t.getRevokeReason() != RevokeReason.ROTATED);
        }

        @Override
        public void revokeFamily(UUID familyId, RevokeReason reason, Instant now) {
            byFamily(familyId).forEach(t -> t.revoke(reason, now));
        }

        @Override
        public void revokeAllByUser(UUID userId, RevokeReason reason, Instant now) {
            tokens.stream().filter(t -> t.getUserId().equals(userId)).forEach(t -> t.revoke(reason, now));
        }

        @Override
        public int deleteExpiredBefore(Instant instant) {
            int before = tokens.size();
            tokens.removeIf(t -> t.getExpiresAt().isBefore(instant));
            return before - tokens.size();
        }
    }
}
