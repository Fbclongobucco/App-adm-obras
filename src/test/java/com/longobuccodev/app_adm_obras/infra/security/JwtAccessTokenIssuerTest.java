package com.longobuccodev.app_adm_obras.infra.security;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.security.AccessTokenIssuer.IssuedAccessToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAccessTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

    private final JwtProperties properties = new JwtProperties("app-adm-obras", JwtPropertiesTest.VALID_SECRET,
            Duration.ofMinutes(15), Duration.ofDays(7), null);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final SecurityConfig config = new SecurityConfig();
    private final JwtAccessTokenIssuer issuer =
            new JwtAccessTokenIssuer(config.jwtEncoder(properties), properties, clock);
    private final JwtDecoder decoder = config.jwtDecoder(properties, clock);

    private static User admin() {
        User user = new User(null, "Ana Souza", "ana@email.com", true, "$2a$10$abcdefghijklmnopqrstuv");
        user.addRole(Role.ADMIN);
        return user;
    }

    @Test
    @DisplayName("token deve ter emissor, sub com id, roles e validade de 15 min")
    void tokenShouldCarryExpectedClaims() {
        User user = admin();

        IssuedAccessToken issued = issuer.issue(user);
        Jwt jwt = decoder.decode(issued.value());

        assertThat(jwt.getClaimAsString("iss")).isEqualTo("app-adm-obras");
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(issued.issuedAt()).isEqualTo(jwt.getIssuedAt());
        assertThat(issued.expiresAt()).isEqualTo(jwt.getExpiresAt());
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    @DisplayName("token nao deve conter email, nome nem hash de senha")
    void tokenShouldCarryNoPersonalDataOrSecret() {
        User user = admin();

        Jwt jwt = decoder.decode(issuer.issue(user).value());
        String payload = new String(Base64.getUrlDecoder().decode(jwt.getTokenValue().split("\\.")[1]),
                StandardCharsets.UTF_8);

        assertThat(jwt.getClaims()).containsOnlyKeys("iss", "sub", "iat", "exp", "roles");
        assertThat(payload).doesNotContain("ana@email.com").doesNotContain("Ana Souza").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("token com payload alterado deve ser rejeitado")
    void tamperedTokenShouldBeRejected() {
        String[] parts = issuer.issue(admin()).value().split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"iss\":\"app-adm-obras\",\"sub\":\"x\",\"roles\":[\"ADMIN\"],\"exp\":4102444800}"
                        .getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> decoder.decode(parts[0] + "." + forgedPayload + "." + parts[2]))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("token expirado deve ser rejeitado")
    void expiredTokenShouldBeRejected() {
        String value = issuer.issue(admin()).value();
        JwtDecoder later = config.jwtDecoder(properties, Clock.fixed(NOW.plus(Duration.ofMinutes(20)), ZoneOffset.UTC));

        assertThatThrownBy(() -> later.decode(value)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("token de outro emissor deve ser rejeitado")
    void otherIssuerShouldBeRejected() {
        JwtProperties other = new JwtProperties("outro-sistema", JwtPropertiesTest.VALID_SECRET, null, null, null);
        String value = new JwtAccessTokenIssuer(config.jwtEncoder(other), other, clock).issue(admin()).value();

        assertThatThrownBy(() -> decoder.decode(value)).isInstanceOf(JwtException.class);
    }
}
