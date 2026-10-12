package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidRestaurantException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestaurantTest {

    private static Project project() {
        return new Project(null, "OS-200", null, new CostCenter(null, "Centro Norte", "11222333000181"),
                LocalDate.of(2026, 1, 5), null,
                new Client(null, "Cliente Alfa", "alfa@exemplo.test", "1133334444",
                        new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000")),
                false);
    }

    @Test
    @DisplayName("deve normalizar nome e telefone e aceitar telefone e endereco ausentes")
    void shouldNormalizeAndAcceptOptionalFields() {
        Restaurant restaurant = new Restaurant(null, "  Cantina   da  Obra ", "(19) 3333-6666", null, project());
        Restaurant minimal = new Restaurant(null, "Bar", null, null, project());

        assertThat(restaurant.getName()).isEqualTo("Cantina da Obra");
        assertThat(restaurant.getPhone()).isEqualTo("1933336666");
        assertThat(minimal.getPhone()).isNull();
        assertThat(minimal.getAddress()).isNull();
    }

    @Test
    @DisplayName("nome com 1 ou mais de 100 caracteres deve ser rejeitado com restaurant.invalid.name")
    void shouldRejectInvalidName() {
        assertThatThrownBy(() -> new Restaurant(null, "A", null, null, project()))
                .isInstanceOf(InvalidRestaurantException.class)
                .extracting("errorCode").isEqualTo("restaurant.invalid.name");
        assertThatThrownBy(() -> new Restaurant(null, "a".repeat(101), null, null, project()))
                .extracting("errorCode").isEqualTo("restaurant.invalid.name");
    }

    @Test
    @DisplayName("telefone com quantidade de digitos errada deve ser rejeitado com restaurant.invalid.phone")
    void shouldRejectInvalidPhone() {
        assertThatThrownBy(() -> new Restaurant(null, "Cantina", "1234", null, project()))
                .isInstanceOf(InvalidRestaurantException.class)
                .extracting("errorCode").isEqualTo("restaurant.invalid.phone");
    }

    @Test
    @DisplayName("restaurante sem obra deve ser rejeitado com restaurant.invalid.project")
    void shouldRequireProject() {
        assertThatThrownBy(() -> new Restaurant(null, "Cantina", null, null, null))
                .isInstanceOf(InvalidRestaurantException.class)
                .extracting("errorCode").isEqualTo("restaurant.invalid.project");
    }
}
