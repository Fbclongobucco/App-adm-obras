package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidRestaurantException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
    @DisplayName("taxa de entrega e opcional; informada, deve ser maior que zero")
    void shouldAcceptOptionalDeliveryFee() {
        Restaurant withoutFee = new Restaurant(null, "Cantina", null, null, project());
        Restaurant withFee = new Restaurant(null, "Cantina", null, null, project(), new BigDecimal("8.00"));

        assertThat(withoutFee.getLunchDeliveryFee()).isNull();
        assertThat(withoutFee.getDinnerDeliveryFee()).isNull();
        assertThat(withFee.getLunchDeliveryFee()).isEqualByComparingTo("8.00");
        assertThat(withFee.getDinnerDeliveryFee()).isEqualByComparingTo("8.00");

        withFee.setDinnerDeliveryFee(null);
        assertThat(withFee.getLunchDeliveryFee()).isEqualByComparingTo("8.00");
        assertThat(withFee.getDinnerDeliveryFee()).isNull();
    }

    @Test
    @DisplayName("taxa de entrega zero ou negativa deve ser rejeitada com restaurant.invalid.delivery_fee")
    void shouldRejectDeliveryFeeNotGreaterThanZero() {
        for (BigDecimal fee : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-1.00")}) {
            assertThatThrownBy(() -> new Restaurant(null, "Cantina", null, null, project(), fee))
                    .isInstanceOf(InvalidRestaurantException.class)
                    .extracting("errorCode").isEqualTo("restaurant.invalid.delivery_fee");
        }
        Restaurant restaurant = new Restaurant(null, "Cantina", null, null, project(), new BigDecimal("8.00"));
        assertThatThrownBy(() -> restaurant.setLunchDeliveryFee(BigDecimal.ZERO))
                .extracting("errorCode").isEqualTo("restaurant.invalid.delivery_fee");
        assertThatThrownBy(() -> restaurant.setDinnerDeliveryFee(new BigDecimal("-1.00")))
                .extracting("errorCode").isEqualTo("restaurant.invalid.delivery_fee");
        assertThat(restaurant.getLunchDeliveryFee()).isEqualByComparingTo("8.00");
        assertThat(restaurant.getDinnerDeliveryFee()).isEqualByComparingTo("8.00");
    }

    @Test
    @DisplayName("refrigerante e opcional; informado, deve ser maior que zero")
    void shouldAcceptOptionalSodaPrice() {
        Restaurant withoutSoda = new Restaurant(null, "Cantina", null, null, project(), new BigDecimal("8.00"));
        Restaurant withSoda = new Restaurant(null, "Cantina", null, null, project(), null, new BigDecimal("5.00"));

        assertThat(withoutSoda.getSodaPrice()).isNull();
        assertThat(withSoda.getSodaPrice()).isEqualByComparingTo("5.00");
        assertThat(withSoda.getLunchDeliveryFee()).isNull();

        withSoda.setSodaPrice(null);
        assertThat(withSoda.getSodaPrice()).isNull();
    }

    @Test
    @DisplayName("refrigerante zero ou negativo deve ser rejeitado com restaurant.invalid.soda_price")
    void shouldRejectSodaPriceNotGreaterThanZero() {
        for (BigDecimal price : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-1.00")}) {
            assertThatThrownBy(() -> new Restaurant(null, "Cantina", null, null, project(), null, price))
                    .isInstanceOf(InvalidRestaurantException.class)
                    .extracting("errorCode").isEqualTo("restaurant.invalid.soda_price");
        }
        Restaurant restaurant = new Restaurant(null, "Cantina", null, null, project(), null, new BigDecimal("5.00"));
        assertThatThrownBy(() -> restaurant.setSodaPrice(BigDecimal.ZERO))
                .extracting("errorCode").isEqualTo("restaurant.invalid.soda_price");
        assertThat(restaurant.getSodaPrice()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("restaurante sem obra deve ser rejeitado com restaurant.invalid.project")
    void shouldRequireProject() {
        assertThatThrownBy(() -> new Restaurant(null, "Cantina", null, null, null))
                .isInstanceOf(InvalidRestaurantException.class)
                .extracting("errorCode").isEqualTo("restaurant.invalid.project");
    }
}
