package com.longobuccodev.app_adm_obras.infra.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshCookiePropertiesTest {

    @Test
    @DisplayName("sem configuracao, o cookie e Secure, Strict e nao particionado")
    void defaults() {
        RefreshCookieProperties properties = new RefreshCookieProperties(null, null, null);

        assertThat(properties.secure()).isTrue();
        assertThat(properties.sameSite()).isEqualTo("Strict");
        assertThat(properties.partitioned()).isFalse();
    }

    @ParameterizedTest(name = "{0} deve virar {1}")
    @CsvSource({"Strict,Strict", "lax,Lax", "NONE,None"})
    @DisplayName("SameSite deve ser aceito sem diferenciar maiusculas")
    void sameSiteShouldBeCaseInsensitive(String input, String expected) {
        assertThat(new RefreshCookieProperties(true, input, false).sameSite()).isEqualTo(expected);
    }

    @Test
    @DisplayName("SameSite desconhecido deve falhar nomeando a variavel")
    void unknownSameSiteShouldFail() {
        assertThatThrownBy(() -> new RefreshCookieProperties(true, "Foo", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_REFRESH_COOKIE_SAME_SITE");
    }

    @Test
    @DisplayName("SameSite=None sem Secure deve falhar nomeando as duas variaveis")
    void noneWithoutSecureShouldFail() {
        assertThatThrownBy(() -> new RefreshCookieProperties(false, "None", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_REFRESH_COOKIE_SAME_SITE")
                .hasMessageContaining("APP_REFRESH_COOKIE_SECURE");
    }

    @Test
    @DisplayName("SameSite=None com Secure deve ser aceito")
    void noneWithSecureShouldBeAccepted() {
        RefreshCookieProperties properties = new RefreshCookieProperties(true, "None", true);

        assertThat(properties.sameSite()).isEqualTo("None");
        assertThat(properties.partitioned()).isTrue();
    }
}
