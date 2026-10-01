package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidAccommodationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccommodationTest {

    private static Address newAddress() {
        return new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100");
    }

    private static Accommodation newAccommodation() {
        return new Accommodation(null, " Maria  Souza ", "(11) 98888-7777", newAddress(), 8, 30,
                null, null, null, new BigDecimal("4500.00"));
    }

    @Test
    void shouldNormalizeFieldsOnConstructor() {
        Accommodation accommodation = newAccommodation();

        assertThat(accommodation.getId()).isNotNull();
        assertThat(accommodation.getHostName()).isEqualTo("Maria Souza");
        assertThat(accommodation.getHostPhone()).isEqualTo("11988887777");
        assertThat(accommodation.getCapacity()).isEqualTo(8);
        assertThat(accommodation.getDays()).isEqualTo(30);
        assertThat(accommodation.getEmployees()).isEmpty();
        assertThat(accommodation.getIsContract()).isFalse();
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();

        assertThat(new Accommodation(id, "Maria Souza", "11988887777", newAddress(), 8, 30,
                true, null, null, BigDecimal.TEN).getId()).isEqualTo(id);
        assertThat(newAccommodation().getId()).isNotNull();
    }

    @Test
    void shouldCopyEmployeesOnConstructor() {
        Set<Employee> employees = new HashSet<>();
        employees.add(new Employee(null, "Joao da Silva", "joao@email.com", "529.982.247-25", null, null,
                java.time.LocalDate.of(1990, 5, 20),
                new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81"), null, Employee.Role.MONTADOR));

        Accommodation accommodation = new Accommodation(null, " Maria  Souza ", "(11) 98888-7777",
                newAddress(), 8, 30, null, null, employees, new BigDecimal("4500.00"));
        employees.clear();

        assertThat(accommodation.getEmployees()).hasSize(1);
    }

    @Test
    void shouldRejectBlankHostName() {
        Accommodation accommodation = newAccommodation();

        assertThatThrownBy(() -> accommodation.setHostName(" "))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.host_name.blank");
    }

    @Test
    void shouldRejectShortHostName() {
        Accommodation accommodation = newAccommodation();

        assertThatThrownBy(() -> accommodation.setHostName("Ma"))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.host_name.length");
    }

    @Test
    void shouldRejectInvalidHostPhone() {
        Accommodation accommodation = newAccommodation();

        assertThatThrownBy(() -> accommodation.setHostPhone("7777"))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.host_phone");
    }

    @Test
    void shouldRejectMissingAddress() {
        Accommodation accommodation = newAccommodation();

        assertThatThrownBy(() -> accommodation.setAddress(null))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.address");
    }

    @Test
    void shouldRejectInvalidCapacity() {
        Accommodation accommodation = newAccommodation();

        assertThatThrownBy(() -> accommodation.setCapacity(0))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.capacity");
        assertThatThrownBy(() -> accommodation.setCapacity(1001))
                .isInstanceOf(InvalidAccommodationException.class);
        assertThatThrownBy(() -> accommodation.setCapacity(null))
                .isInstanceOf(InvalidAccommodationException.class);
    }

    @Test
    void shouldRejectInvalidDays() {
        Accommodation accommodation = newAccommodation();

        assertThatThrownBy(() -> accommodation.setDays(0))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.days");
        assertThatThrownBy(() -> accommodation.setDays(366))
                .isInstanceOf(InvalidAccommodationException.class);
    }

    @Test
    void shouldRejectNegativeTotalPrice() {
        Accommodation accommodation = newAccommodation();

        assertThatThrownBy(() -> accommodation.setTotalPrice(new BigDecimal("-0.01")))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.total_price");
        assertThatThrownBy(() -> accommodation.setTotalPrice(null))
                .isInstanceOf(InvalidAccommodationException.class);
    }

    @Test
    void shouldDefaultContractToFalse() {
        Accommodation accommodation = newAccommodation();

        accommodation.setContract(true);
        assertThat(accommodation.isContract()).isTrue();

        accommodation.setContract(null);
        assertThat(accommodation.isContract()).isFalse();
    }

    @Test
    void shouldValidateOnConstructor() {
        assertThatThrownBy(() -> new Accommodation(null, null, "11988887777", newAddress(), 8, 30,
                false, null, null, BigDecimal.TEN))
                .isInstanceOf(InvalidAccommodationException.class);
    }
}