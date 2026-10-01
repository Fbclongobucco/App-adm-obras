package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidAddressException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressTest {

    private static Address newAddress() {
        return new Address(null, "Rua das Flores", "  120 ", "Sao Paulo", " sp ", " Brasil ",
                " Centro ", "01310-100");
    }

    @Test
    void shouldNormalizeFieldsOnConstructor() {
        Address address = newAddress();

        assertThat(address.getId()).isNotNull();
        assertThat(address.getStreet()).isEqualTo("Rua das Flores");
        assertThat(address.getNumber()).isEqualTo("120");
        assertThat(address.getCity()).isEqualTo("Sao Paulo");
        assertThat(address.getState()).isEqualTo("SP");
        assertThat(address.getCountry()).isEqualTo("Brasil");
        assertThat(address.getNeighborhood()).isEqualTo("Centro");
        assertThat(address.getZipCode()).isEqualTo("01310100");
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();
        Address address = newAddress();

        assertThat(new Address(id, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100")
                .getId()).isEqualTo(id);

        address.setId(null);

        assertThat(address.getId()).isNotNull();
    }

    @Test
    void shouldCompareById() {
        UUID id = UUID.randomUUID();
        Address first = newAddress();
        Address second = newAddress();

        assertThat(first).isNotEqualTo(second);

        first.setId(id);
        second.setId(id);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldRejectBlankStreet() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setStreet(" "))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.street");
    }

    @Test
    void shouldRejectLongStreet() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setStreet("a".repeat(101)))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.street");
    }

    @Test
    void shouldRejectBlankNumber() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setNumber(null))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.number");
    }

    @Test
    void shouldAcceptNumberWithoutDigit() {
        Address address = newAddress();

        address.setNumber("S/N");

        assertThat(address.getNumber()).isEqualTo("S/N");
    }

    @Test
    void shouldRejectBlankCity() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setCity(""))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.city");
    }

    @Test
    void shouldRejectBlankState() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setState(" "))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.state.blank");
    }

    @Test
    void shouldRejectInvalidState() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setState("Sao Paulo"))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.state");
        assertThatThrownBy(() -> address.setState("S1"))
                .isInstanceOf(InvalidAddressException.class);
    }

    @Test
    void shouldRejectBlankCountry() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setCountry(null))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.country");
    }

    @Test
    void shouldRejectBlankNeighborhood() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setNeighborhood("  "))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.neighborhood");
    }

    @Test
    void shouldRejectInvalidZipCode() {
        Address address = newAddress();

        assertThatThrownBy(() -> address.setZipCode("0131010"))
                .isInstanceOf(InvalidAddressException.class)
                .extracting("errorCode").isEqualTo("address.invalid.zip_code");
        assertThatThrownBy(() -> address.setZipCode(null))
                .isInstanceOf(InvalidAddressException.class);
    }

    @Test
    void shouldValidateOnConstructor() {
        assertThatThrownBy(() -> new Address(null, null, "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"))
                .isInstanceOf(InvalidAddressException.class);
    }
}