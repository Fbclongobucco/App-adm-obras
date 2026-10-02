package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.AccommodationRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.AccommodationResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Address;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.exception.InvalidAccommodationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccommodationMapperTest {

    private static AddressRequestDTO newAddressDto() {
        return new AddressRequestDTO("Rua das Flores", "120", "Sao Paulo", "sp", "Brasil", "Centro", "01310-100");
    }

    private static AccommodationRequestDTO newRequest(AddressRequestDTO address) {
        return new AccommodationRequestDTO(" Maria  Souza ", "(11) 98888-7777", address, 8, 30, true,
                null, null, new BigDecimal("4500.00"));
    }

    private static Employee newEmployee() {
        return new Employee(null, "Joao da Silva", "joao@email.com", "529.982.247-25", null, null,
                LocalDate.of(1990, 5, 20), new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81"),
                Employee.Role.MONTADOR);
    }

    private static Project newProject() {
        Client client = new Client(null, "Construtora Alfa", "contato@alfa.com", "1133334444",
                new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"));
        return new Project(null, "OS-1234", "Obra de reforma",
                new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81"), LocalDate.of(2026, 1, 10), null,
                client, false);
    }

    @Test
    void shouldMapRequestToDomain() {
        Employee employee = newEmployee();

        Accommodation accommodation = AccommodationMapper.toDomain(newRequest(newAddressDto()), null,
                Set.of(employee));

        assertThat(accommodation.getId()).isNotNull();
        assertThat(accommodation.getHostName()).isEqualTo("Maria Souza");
        assertThat(accommodation.getHostPhone()).isEqualTo("11988887777");
        assertThat(accommodation.getAddress().getState()).isEqualTo("SP");
        assertThat(accommodation.getAddress().getZipCode()).isEqualTo("01310100");
        assertThat(accommodation.getCapacity()).isEqualTo(8);
        assertThat(accommodation.getDays()).isEqualTo(30);
        assertThat(accommodation.isContract()).isTrue();
        assertThat(accommodation.getEmployees()).containsExactly(employee);
        assertThat(accommodation.getTotalPrice()).isEqualByComparingTo("4500.00");
    }

    @Test
    void shouldKeepIdsWhenMappingOverExisting() {
        Accommodation existing = AccommodationMapper.toDomain(newRequest(newAddressDto()), null, null);

        Accommodation updated = AccommodationMapper.toDomain(existing, newRequest(newAddressDto()), null, null);

        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getAddress().getId()).isEqualTo(existing.getAddress().getId());
    }

    @Test
    void shouldRejectMissingAddress() {
        assertThatThrownBy(() -> AccommodationMapper.toDomain(newRequest(null), null, null))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.address");
    }

    @Test
    void shouldMapDomainToResponseWithNestedObjects() {
        Employee employee = newEmployee();
        Project project = newProject();
        Accommodation accommodation = AccommodationMapper.toDomain(newRequest(newAddressDto()), project,
                Set.of(employee));

        AccommodationResponseDTO response = AccommodationMapper.toResponse(accommodation);

        assertThat(response.id()).isEqualTo(accommodation.getId());
        assertThat(response.hostName()).isEqualTo("Maria Souza");
        assertThat(response.address().id()).isEqualTo(accommodation.getAddress().getId());
        assertThat(response.address().zipCode()).isEqualTo("01310100");
        assertThat(response.project().id()).isEqualTo(project.getId());
        assertThat(response.project().os()).isEqualTo("OS-1234");
        assertThat(response.project().costCenter().name()).isEqualTo("Obra Sao Paulo");
        assertThat(response.project().client().name()).isEqualTo("Construtora Alfa");
        assertThat(response.employees()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(employee.getId());
            assertThat(summary.name()).isEqualTo("Joao da Silva");
            assertThat(summary.costCenter().cnpj()).isEqualTo("11222333000181");
        });
        assertThat(response.totalPrice()).isEqualByComparingTo("4500.00");
    }

    @Test
    void shouldMapResponseWithoutProject() {
        Accommodation accommodation = AccommodationMapper.toDomain(newRequest(newAddressDto()), null, null);

        AccommodationResponseDTO response = AccommodationMapper.toResponse(accommodation);

        assertThat(response.project()).isNull();
        assertThat(response.employees()).isEmpty();
    }
}
